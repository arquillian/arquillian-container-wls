/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.wls.remote_103x;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;

/**
 * A JMX client that connects to the Domain Runtime MBean Server
 * to obtain information about Arquillian deployments, as well as
 * the state of the target server for deployment.
 * 
 * This JMX client relies on Oracle WebLogic's implementation of HTTP and IIOP protocols,
 * while also supporting the T3 protocol (as IIOP).
 * 
 * Details in this area are covered by the Oracle Fusion Middleware Guide on
 * "Developing Custom Management Utilities With JMX for Oracle WebLogic Server". 
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicJMXClient
{

   private static final Logger logger = Logger.getLogger(WebLogicJMXClient.class.getName());
   
   private static final String RUNNING = "RUNNING";
   
   private WebLogicConfiguration configuration;
   
   private MBeanServerConnection connection;

   private JMXConnector connector;
   
   private ObjectName domainRuntimeService;

   public WebLogicJMXClient(WebLogicConfiguration configuration)
   {
      this.configuration = configuration;
      try
      {
         this.domainRuntimeService = new ObjectName("com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
      }
      catch (MalformedObjectNameException objectNameEx)
      {
         // We're pretty much in trouble now. The constructed object will be useless.
         throw new IllegalStateException(objectNameEx);
      }
   }

   /**
    * Verifies and obtains details of the deployment.
    * 
    * @param deploymentName The name of the deployment
    * @return A {@link ProtocolMetaData} object containing details of the deployment. 
    * @throws DeploymentException When there is a failure obtaining details of the deployment from the Domain Runtime MBean server.
    */
   public ProtocolMetaData deploy(String deploymentName) throws DeploymentException
   {
      try
      {
         // Sets the thread's context classloader to load the WLS libs.
         initWebLogicJMXLibClassLoader();
         // Now, create a connection to the Domain Runtime MBean Server.
         createConnection();

         ProtocolMetaData metadata = new ProtocolMetaData();
         HTTPContext context = getTargetContextInfo();
         try
         {
            populateContext(deploymentName, context);
         }
         catch (Exception ex)
         {
            throw new DeploymentException("Failed to populate the HTTPContext with the deployment details", ex);
         }

         metadata.addContext(context);
         return metadata;
      }
      finally
      {
         // Close the connection first.
         closeConnection();
         // Reset the thread's context classloader back to the original. 
         destroyWebLogicJMXLibClassLoader();
      }
   }
   
   /**
    * Verifies that the application was undeployed.
    * We do not want a subsequent deployment with the same name to fail. 
    * 
    * @param deploymentName The name of the deployment
    * @throws DeploymentException When there is a failure obtaining details of the deployment from the Domain Runtime MBean server.
    */
   public void undeploy(String deploymentName) throws DeploymentException
   {
      try
      {
         // Sets the thread's context classloader to load the WLS libs.
         initWebLogicJMXLibClassLoader();
         // Now, create a connection to the Domain Runtime MBean Server.
         createConnection();
         
         verifyUndeploymentStatus(deploymentName);
      }
      catch (Exception ex)
      {
         throw new DeploymentException("Failed to obtain the status of the deployment.", ex);
      }
      finally
      {
         // Close the connection first.
         closeConnection();
         // Reset the thread's context classloader back to the original.
         destroyWebLogicJMXLibClassLoader();
      }
   }

   /**
    * Creates a new {@link HTTPContext} object with the details of the target server.
    * Also verifies that the target server is running.
    * 
    * @return The created {@link HTTPContext}
    * @throws DeploymentException When the {@link HTTPContext} could not be created.
    */
   private HTTPContext getTargetContextInfo() throws DeploymentException
   {
      HTTPContext httpContext = null;
      try
      {
         ObjectName[] wlServerRuntimes = getWLServerRuntimes();
         for (ObjectName wlServerRuntime : wlServerRuntimes)
         {
            String serverName = (String) connection.getAttribute(wlServerRuntime, "Name");
            if (serverName.equals(configuration.getTarget()))
            {
               String serverState = (String) connection.getAttribute(wlServerRuntime, "State");
               if(!serverState.equals(RUNNING))
               {
                  throw new DeploymentException("The designated target server is not in the RUNNING state. Cannot proceed to execute the tests.");
               }
               
               String httpUrlAsString = (String) connection.invoke(wlServerRuntime, "getURL", new Object[]{"http"}, new String[]{"java.lang.String"});
               URL serverHttpUrl = new URL(httpUrlAsString);
               httpContext = new HTTPContext(serverHttpUrl.getHost(), serverHttpUrl.getPort());
            }
         }
      }
      catch (Exception ex)
      {
         throw new DeploymentException("Failed to create the HTTPContext for the deployment", ex);
      }
      if(httpContext ==null)
      {
         throw new DeploymentException("Failed to obtain a HTTPContext for the deployment. Possible causes include:" +
         		"1. The target specified in arquillian.xml is not a valid WebLogic Server deployment target." +
         		"2. The non-SSL listen port is not enabled for the target.");
      }
      return httpContext;
   }
   
   /**
    * Populates the {@link HTTPContext} object with details of the servlets associated with the deployment.
    * 
    * @param deploymentName The name of the deployed application
    * @param context A {@link HTTPContext} object that already contains details of the target server.
    * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
    */
   private void populateContext(String deploymentName, HTTPContext context) throws Exception
   {
      ObjectName[] wlServerRuntimes = getWLServerRuntimes();
      for (ObjectName wlServerRuntime: wlServerRuntimes)
      {
         String serverName = (String) connection.getAttribute(wlServerRuntime, "Name");
         if (serverName.equals(configuration.getTarget()))
         {
            ObjectName[] applicationRuntimes = (ObjectName[]) connection.getAttribute(wlServerRuntime, "ApplicationRuntimes");
            boolean foundAppInfo = false;
            for(ObjectName applicationRuntime: applicationRuntimes)
            {
               String applicationName = (String) connection.getAttribute(applicationRuntime, "Name");
               if(applicationName.equals(deploymentName))
               {
                  foundAppInfo = true;
                  ObjectName[] componentRuntimes = (ObjectName[]) connection.getAttribute(applicationRuntime, "ComponentRuntimes");
                  for(ObjectName componentRuntime : componentRuntimes)
                  {
                     String componentType = (String) connection.getAttribute(componentRuntime, "Type");
                     if (componentType.toString().equals("WebAppComponentRuntime"))
                     {
                        ObjectName[] servletRuntimes = (ObjectName[]) connection.getAttribute(componentRuntime, "Servlets");
                        for(ObjectName servletRuntime: servletRuntimes)
                        {
                           String servletName = (String) connection.getAttribute(servletRuntime, "ServletName");
                           String servletContextRoot = (String) connection.getAttribute(servletRuntime, "ContextPath");
                           context.add(new Servlet(servletName, servletContextRoot));
                        }
                     }
                  }
               }
            }
            if(foundAppInfo == false)
            {
               throw new DeploymentException("Failed to find the deployed application."); 
            }
         }
      }
   }
   
   /**
    * Verifies that the application has been undeployed.
    * 
    * @param deploymentName The name of the application that was undeployed. 
    * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
    */
   private void verifyUndeploymentStatus(String deploymentName) throws Exception
   {
      ObjectName[] wlServerRuntimes = getWLServerRuntimes();
      for (ObjectName wlServerRuntime: wlServerRuntimes)
      {
         String serverName = (String) connection.getAttribute(wlServerRuntime, "Name");
         if (serverName.equals(configuration.getTarget()))
         {
            ObjectName[] applicationRuntimes = (ObjectName[]) connection.getAttribute(wlServerRuntime, "ApplicationRuntimes");
            boolean foundAppInfo = false;
            for(ObjectName applicationRuntime: applicationRuntimes)
            {
               String applicationName = (String) connection.getAttribute(applicationRuntime, "Name");
               if(applicationName.equals(deploymentName))
               {
                  foundAppInfo = true;
                  break;
               }
            }
            if(foundAppInfo == true)
            {
               throw new DeploymentException("Failed to undeploy the deployed application."); 
            }
         }
      }
   }
   
   private ObjectName[] getWLServerRuntimes() throws Exception
   {
      return (ObjectName[]) connection.getAttribute(domainRuntimeService, "ServerRuntimes");
   }

   /**
    * Sets the thread's context classloader to a an instance of {@link WebLogicJMXLibClassLoader},
    * that has the wljmxclient.jar from WL_HOME as a codesource.
    * The original context classloader of the thread is the parent of the new classloader,
    * and all classes to be loaded will be delegated to the parent first,
    * and then searched for in wljmxclient.jar (and associated classes in the Manifest).
    * 
    * This method is preferably invoked as late as possible.
    */
   private void initWebLogicJMXLibClassLoader()
   {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if(contextClassLoader.getClass().equals(WebLogicJMXLibClassLoader.class))
      {
         // Our thread's context classloader is already the WebLogicJMXLibClassLoader.
         // We do not need to create a child classloader.
         logger.fine("The thread's context classloader has already been set to the desired classloader.");
         return;
      }

      File wlHome =  new File(configuration.getJmxLibPath());
      try
      {
         URL[] urls = { wlHome.toURI().toURL() };
         ClassLoader jmxLibraryClassLoader = new WebLogicJMXLibClassLoader(urls, contextClassLoader);
         Thread.currentThread().setContextClassLoader(jmxLibraryClassLoader);
      }
      catch (MalformedURLException urlEx)
      {
         throw new RuntimeException("The constructed path to wljmxclient.jar appears to be invalid. Verify that you have access to this jar and it's dependencies.", urlEx);
      }
   }
   
   /**
    * Unsets the thread's context classloader to the original parent,
    * from the existing {@link WebLogicJMXLibClassLoader}.
    * We'll do this to ensure that Arquillian tests may run unaffected,
    * if the {@link WebLogicJMXLibClassLoader} were to interfere somehow.
    * 
    * This method is preferably invoked as soon as possible.
    */
   private void destroyWebLogicJMXLibClassLoader()
   {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if(!contextClassLoader.getClass().equals(WebLogicJMXLibClassLoader.class))
      {
         // Our thread's context classloader is no longer the WebLogicJMXLibClassLoader.
         // We'll not reset the classloader to the original parent.
         logger.fine("The thread's context classloader was not the WebLogicJMXClassLoader.");
         return;
      }
      else
      {
         ClassLoader originalParentClassLoader = contextClassLoader.getParent();
         Thread.currentThread().setContextClassLoader(originalParentClassLoader);
      }
   }
   
   /**
    * Initialize connection to the Domain Runtime MBean Server
    * 
    * @throws DeploymentException When a connection to the Domain Runtime MBean Server could not be established.
    */
   private void createConnection() throws DeploymentException
   {
      initWebLogicJMXLibClassLoader();
      String protocol = configuration.getProtocol();
      String hostname = configuration.getAdminListenAddress();
      int portNum = Integer.valueOf(configuration.getAdminListenPort());
      String domainRuntimeMBeanServerURL = "/jndi/weblogic.management.mbeanservers.domainruntime";

      try
      {
         JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname, portNum, domainRuntimeMBeanServerURL);
         Map<String, String> props = new HashMap<String, String>();
         props.put(Context.SECURITY_PRINCIPAL, configuration.getAdminUserName());
         props.put(Context.SECURITY_CREDENTIALS, configuration.getAdminPassword());
         props.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
         connector = JMXConnectorFactory.connect(serviceURL, props);
         connection = connector.getMBeanServerConnection();
      }
      catch (IOException ioEx)
      {
         throw new DeploymentException("Failed to obtain a connection to the MBean Server.", ioEx);
      }
   }
   
   /**
    * Closes the connection to the Domain Runtime MBean Server.
    */
   private void closeConnection()
   {
      try
      {
         connector.close();
      }
      catch (IOException ioEx)
      {
         logger.log(Level.FINE, "Failed to close the connection to the MBean Server.", ioEx);
      }
   }

}
