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
package org.jboss.arquillian.container.wls;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
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

   /**
    * A utility class that encapsulates the logic for creation of a {@link HTTPContext} instance.
    *  
    * @author Vineet Reynolds
    *
    */
   private class HTTPContextBuilder
   {
      /**
       * The context that is created. This will be returned to the client, once it is completely built.
       */
      private HTTPContext httpContext;
      
      /**
       * The deployment for which the context must be built.
       */
      private String deploymentName;
      
      /**
       * The set of Server Runtime MBeans to use for preparing the context.
       * This will be one in the case of a deployment against a single managed server,
       * and multiple for a deployment against a cluster.
       */
      private ObjectName[] wlServerRuntimes;
      
      public HTTPContextBuilder(String deploymentName)
      {
         this.deploymentName = deploymentName;
      }

      public HTTPContext createContext() throws Exception
      {
         // First, get the deployment in the domain configuration
         // that matches the deployment made by Arquillian.
         ObjectName appDeployment = findMatchingDeployment(deploymentName);
         if(appDeployment == null)
         {
            throw new DeploymentException("The specified deployment could not be found in the MBean Server.\n"
                  + "The deployment must have failed. Verify the output of the weblogic.Deployer process.");
         }
         // Get the targets for the deployment. For now, there will be a single target
         // This will be either a managed server or a cluster.
         ObjectName[] targets = (ObjectName[]) connection.getAttribute(appDeployment, "Targets");
         for (ObjectName target : targets)
         {
            String targetType = (String) connection.getAttribute(target, "Type");
            String targetName = (String) connection.getAttribute(target, "Name");
            if (targetName.equals(configuration.getTarget()))
            {
               if (targetType.equals("Server"))
               {
                  // Get the Server Runtime MBean, that will be used to create the context.
                  wlServerRuntimes = findRunningWLServerRuntimes(targetName);
                  buildHTTPContext();
               }
               else if (targetType.equals("Cluster"))
               {
                  // Get all the Server Runtime MBeans for the servers in the cluster,
                  // that will be used to create the context.
                  String[] clusterMemberNames = findMembersOfCluster(target);
                  wlServerRuntimes = findRunningWLServerRuntimes(clusterMemberNames);
                  buildHTTPContext();
               }
               break;
            }
         }
         if(httpContext == null)
         {
            throw new DeploymentException("An unexpected condition was encountered. The HTTPContext could not be created.");
         }
         else
         {
            return httpContext;
         }
      }

      /**
       * Creates the {@link HTTPContext} instance, with the required preconditions in place.
       * 
       * @throws Exception When an exception is encountered during creation of the context.
       */
      private void buildHTTPContext() throws Exception
      {
         // If there are no running servers, we'll abort as the test cannot be executed.
         if (wlServerRuntimes.length < 1)
         {
            throw new DeploymentException("None of the targets are in the RUNNING state.");
         }
         else
         {
            // For now, we'll use the first server to populate the context.
            // This may change in a future Arquillian release,
            // to allow different strategies for testing a clustered deployment. 
            ObjectName wlServerRuntime = wlServerRuntimes[0];
            String httpUrlAsString = (String) connection.invoke(wlServerRuntime, "getURL", 
                  new Object[] {"http"}, new String[] {"java.lang.String"});
            URL serverHttpUrl = new URL(httpUrlAsString);
            httpContext = new HTTPContext(serverHttpUrl.getHost(), serverHttpUrl.getPort());
            List<ObjectName> servletRuntimes = findServletRuntimes(wlServerRuntime, deploymentName);
            for (ObjectName servletRuntime : servletRuntimes)
            {
               String servletName = (String) connection.getAttribute(servletRuntime, "ServletName");
               String servletContextRoot = (String) connection.getAttribute(servletRuntime, "ContextPath");
               httpContext.add(new Servlet(servletName, servletContextRoot));
            }
         }
      }
      
      /**
       * Retrieves the names of cluster members, so that their Runtime MBeans can be fetched from the
       * Domain Runtime MBean Service.
       * 
       * @param cluster The cluster whose member names are to be fetched
       * @return An array of server names whose membership is in the cluster
       * @throws Exception When a failure is encountered when browsing the Domain Configuration MBean Server hierarchy.
       */
      private String[] findMembersOfCluster(ObjectName cluster) throws Exception
      {
         ObjectName[] servers = (ObjectName[]) connection.getAttribute(cluster, "Servers");
         List<String> clusterServers = new ArrayList<String>();
         for (ObjectName server : servers)
         {
            String serverName = (String) connection.getAttribute(server, "Name");
            clusterServers.add(serverName);
         }
         String[] clusterServerNames = clusterServers.toArray(new String[0]);
         return clusterServerNames;
      }

      /**
       * Returns a set of Runtime MBean instances for the provided WebLogic Server names.
       * This is eventually used to create the HTTPContext instance with the runtime listen address and port,
       * as only running WebLogic Server instances are considered for creation of the HTTPContext.
       * 
       * @param runtimeNames The array of WebLogic Server instances for which the Runtime MBeans must be returned
       * @return An array of {@link ObjectName} instances representing running WebLogic Server instances
       * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
       */
      private ObjectName[] findRunningWLServerRuntimes(String... runtimeNames) throws Exception
      {
         List<String> runtimeNamesList = Arrays.asList(runtimeNames);
         List<ObjectName> wlServerRuntimeList = new ArrayList<ObjectName>();
         ObjectName[] wlServerRuntimes = (ObjectName[]) connection.getAttribute(domainRuntimeService, "ServerRuntimes");
         for (ObjectName wlServerRuntime : wlServerRuntimes)
         {
            String runtimeName = (String) connection.getAttribute(wlServerRuntime, "Name");
            String runtimeState = (String) connection.getAttribute(wlServerRuntime, "State");
            if(runtimeNamesList.contains(runtimeName) && runtimeState.equals(RUNNING))
            {
               wlServerRuntimeList.add(wlServerRuntime);
            }
         }
         return wlServerRuntimeList.toArray(new ObjectName[0]);
      }
      
      /**
       * Retrieves a list of Servlet Runtime MBeans for a deployment against a WebLogic Server instance.
       * This is eventually used to populate the HTTPContext instance with all servlets in the deployment.
       * 
       * @param wlServerRuntime The WebLogic Server runtime instance which houses the deployment
       * @param deploymentName The deployment for which the Servlet Runtime MBeans must be retrieved
       * @return A list of {@link ObjectName} representing Servlet Runtime MBeans for the deployment
       * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
       */
      private List<ObjectName> findServletRuntimes(ObjectName wlServerRuntime, String deploymentName) throws Exception
      {
         ObjectName[] applicationRuntimes = (ObjectName[]) connection.getAttribute(wlServerRuntime, "ApplicationRuntimes");
         for(ObjectName applicationRuntime: applicationRuntimes)
         {
            String applicationName = (String) connection.getAttribute(applicationRuntime, "Name");
            if(applicationName.equals(deploymentName))
            {
               ObjectName[] componentRuntimes = (ObjectName[]) connection.getAttribute(applicationRuntime, "ComponentRuntimes");
               List<ObjectName> servletRuntimes = new ArrayList<ObjectName>();
               for(ObjectName componentRuntime : componentRuntimes)
               {
                  String componentType = (String) connection.getAttribute(componentRuntime, "Type");
                  if (componentType.toString().equals("WebAppComponentRuntime"))
                  {
                     servletRuntimes.addAll(Arrays.asList((ObjectName[]) connection.getAttribute(componentRuntime, "Servlets")));
                  }
               }
               return servletRuntimes;
            }
         }
         throw new DeploymentException(
               "The deployment details were not found in the MBean Server. Possible causes include:\n"
                     + "1. The deployment failed. Review the admin server and the target's log files.\n"
                     + "2. The deployment succeeded partially. The deployment must be the Active state. Instead, it might be in the 'New' state.\n"
                     + "   Verify that the the admin server can connect to the target(s), and that no firewall rules are blocking the traffic on the admin channel.");
      }
   }

   private static final Logger logger = Logger.getLogger(WebLogicJMXClient.class.getName());
   
   private static final String RUNNING = "RUNNING";
   
   private static final ThreadLocal<String> trustStorePath = new ThreadLocal<String>();
   
   private static final ThreadLocal<String> trustStorePassword = new ThreadLocal<String>();
   
   private CommonWebLogicConfiguration configuration;
   
   private MBeanServerConnection connection;

   private JMXConnector connector;
   
   private ObjectName domainRuntimeService;

   private ClassLoader jmxLibraryClassLoader;
   
   public WebLogicJMXClient(CommonWebLogicConfiguration configuration) throws LifecycleException
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
      
      // Store the initial state pre-invocation.
      stashInitialState();
      setupState();
      // Now, create a connection to the Domain Runtime MBean Server.
      initWebLogicJMXLibClassLoader();
      createConnection();
      // Reset the state. Allows tests to rely on the original Thread context classloader and System properties. 
      revertToInitialState();
   }

  private void doDeploy(String deploymentName, File deploymentArchive) throws DeploymentException {
    try {
      ObjectName domainRuntime = null;
      domainRuntime = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainRuntime");
      ObjectName deploymentManager = (ObjectName) connection.getAttribute( domainRuntime, "DeploymentManager");

      ObjectName deploymentProgressObject = (ObjectName) connection.invoke( deploymentManager,
                                                                            "deploy",
                                                                            new Object[] {deploymentName, deploymentArchive.getAbsolutePath(), null},
                                                                            new String[] {String.class.getName(), String.class.getName(), String.class.getName()});
      processDeploymentProgress( deploymentName, deploymentManager, deploymentProgressObject);
    } catch (DeploymentException e) {
      throw e;
    } catch (Exception e) {
      throw new DeploymentException( e.getMessage(), e );
    }
  }

  private void processDeploymentProgress(String appName,
                                         ObjectName deploymentManager,
                                         ObjectName deploymentProgressObject ) throws Exception {
    if ( deploymentProgressObject != null ) {

        try {
          String state = waitForDeployToComplete(deploymentProgressObject, 200 );
          if ( state.equals( "STATE_FAILED" ) ) {
            String[] targets = (String []) connection.getAttribute( deploymentProgressObject, "FailedTargets");
            RuntimeException[] exceptions = ( RuntimeException[]) connection.invoke( deploymentProgressObject,
                                                                                     "getExceptions",
                                                                                     new Object[] {targets[0]},
                                                                                     new String[] {String.class.getName() });
            throw new DeploymentException( "Deployment Failed on server: " + exceptions[0].getMessage(), exceptions[0] );
          }
        } finally {
          connection.invoke(deploymentManager,
                            "removeDeploymentProgressObject",
                            new Object[] { appName },
                            new String[] { "java.lang.String" });
        }
    }
  }

  private String waitForDeployToComplete(ObjectName progressObj, int timeToWaitInSecs) throws Exception {
    for (int i = 0; i < timeToWaitInSecs; i++) {
      String state = ( String ) connection.getAttribute( progressObj, "State" );
      if ("STATE_COMPLETED".equals(state) || "STATE_FAILED".equals(state))
        return state;
      try {
        Thread.currentThread().sleep(1000);
      } catch (InterruptedException ex) {
        //ignore
      }
    }
    return "unkown";
  }

  /**
    * Verifies and obtains details of the deployment.
    * 
    * @param deploymentName The name of the deployment
    * @return A {@link ProtocolMetaData} object containing details of the deployment. 
    * @throws DeploymentException When there is a failure obtaining details of the deployment from the Domain Runtime MBean server.
    */
   public ProtocolMetaData deploy(String deploymentName, File deploymentArchive) throws DeploymentException
   {
     doDeploy(deploymentName, deploymentArchive);

      try
      {
         // Store the initial state pre-invocation.
         stashInitialState();
         setupState();

         try
         {

           ProtocolMetaData metadata = new ProtocolMetaData();
            HTTPContextBuilder builder = new HTTPContextBuilder(deploymentName);
            HTTPContext httpContext = builder.createContext();
            HTTPContext context = httpContext;
            metadata.addContext(context);
            return metadata;
         }
         catch (Exception ex)
         {
            throw new DeploymentException("Failed to populate the HTTPContext with the deployment details", ex);
         }
      }
      finally
      {
         // Reset the state. 
         revertToInitialState();
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
     try {
       ObjectName domainRuntime = null;
       domainRuntime = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainRuntime");
       ObjectName deploymentManager = (ObjectName) connection.getAttribute( domainRuntime, "DeploymentManager");
       ObjectName appDeploymentRuntime = (ObjectName) connection.invoke(deploymentManager,
                                                                        "lookupAppDeploymentRuntime",
                                                                        new Object[]{deploymentName},
                                                                        new String[]{String.class.getName()});
       ObjectName deploymentProgressObject = (ObjectName) connection.invoke( appDeploymentRuntime,
                                                                             "undeploy",
                                                                             new Object[] {},
                                                                             new String[] {});
       processDeploymentProgress( deploymentName, deploymentManager, deploymentProgressObject);
     } catch (DeploymentException e) {
       throw e;
     } catch (Exception e) {
       throw new DeploymentException( e.getMessage(), e );
     }
     finally
     {
        // Reset the state.
        revertToInitialState();
     }
   }
   
   public void close() throws LifecycleException
   {
      stashInitialState();
      setupState();
      
      closeConnection();
      
      revertToInitialState();
   }

   /**
    * Verifies that the application has been undeployed.
    * 
    * @param deploymentName The name of the application that was undeployed. 
    * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
    */
   private void verifyUndeploymentStatus(String deploymentName) throws Exception
   {
      ObjectName deployment = findMatchingDeployment(deploymentName);
      if (deployment != null)
      {
         throw new DeploymentException("Failed to undeploy the deployed application.");
      }
   }
   
   /**
    * Retrieves an Application Deployment MBean for a specified deployment.
    * This may return <code>null</code> if the specified deployment is not found,
    * so that this method may be used by both the deployment and undeployment routines
    * to verify if a deployment is available, or not.
    * 
    * @param deploymentName The deployment whose MBean must be retrieved
    * @return An {@link ObjectName} representing the Application Deployment MBean for the deployment.
    * This returns <code>null</code> if a deployment is not found.
    * 
    * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
    */
   private ObjectName findMatchingDeployment(String deploymentName) throws Exception
   {
      ObjectName[] appDeployments = findAllAppDeployments();
      for (ObjectName appDeployment : appDeployments)
      {
         String appDeploymentName = (String) connection.getAttribute(appDeployment, "Name");
         if(appDeploymentName.equals(deploymentName))
         {
            return appDeployment;
         }
      }
      return null;
   }
   
   /**
    * Obtains all the deployments in a WebLogic domain
    * @return An array of {@link ObjectName} corresponding to all deployments in a WebLogic domain.
    * @throws Exception When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
    */
   private ObjectName[] findAllAppDeployments() throws Exception
   {
      ObjectName domainConfig = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainConfiguration");
      ObjectName[] appDeployments = (ObjectName[]) connection.getAttribute(domainConfig, "AppDeployments");
      return appDeployments;
   }
   
   /**
    * Sets the thread's context classloader to a an instance of {@link WebLogicJMXLibClassLoader},
    * that has the weblogic.jar from WL_HOME as a codesource.
    * The original context classloader of the thread is the parent of the new classloader,
    * and all classes to be loaded will be delegated to the parent first,
    * and then searched for in weblogic.jar (and associated archives in the Manifest).
    * 
    * We have to set the current thread's context classloader, 
    * instead of relying on the "jmx.remote.protocol.provider.class.loader" key
    * with an associated value of an instance of {@link WebLogicJMXLibClassLoader}
    * in the environment specified to {@link JMXConnectorFactory}.
    * Classes like weblogic.jndi.WLInitialContextFactory will be loaded by the thread's
    * context classloader and not by the classloader used to load the JMX provider. 
    * 
    * This method is preferably invoked as late as possible.
    */
   private void initWebLogicJMXLibClassLoader()
   {
      File wlHome =  new File(configuration.getJmxClientJarPath());
      try
      {
         URL[] urls = { wlHome.toURI().toURL() };
         jmxLibraryClassLoader = new WebLogicJMXLibClassLoader(urls, Thread.currentThread().getContextClassLoader());
         Thread.currentThread().setContextClassLoader(jmxLibraryClassLoader);
      }
      catch (MalformedURLException urlEx)
      {
         throw new RuntimeException("The constructed path to weblogic.jar appears to be invalid. Verify that you have access to this jar and it's dependencies.", urlEx);
      }
   }
   
   /**
    * Initializes the connection to the Domain Runtime MBean Server
    * 
    * @throws DeploymentException When a connection to the Domain Runtime MBean Server could not be established.
    */
   private void createConnection() throws LifecycleException
   {
      if(connection != null)
      {
         return;
      }
      
      String protocol = configuration.getJmxProtocol();
      String hostname = configuration.getJmxHost();
      int portNum = configuration.getJmxPort();
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
         throw new LifecycleException("Failed to obtain a connection to the MBean Server.", ioEx);
      }
   }
   
   /**
    * Closes the connection to the Domain Runtime MBean Server.
    * @throws LifecycleException 
    */
   private void closeConnection() throws LifecycleException
   {
      try
      {
         if(connector != null)
         {
            connector.close();
         }
      }
      catch (IOException ioEx)
      {
         throw new LifecycleException("Failed to close the connection to the MBean Server.", ioEx);
      }
   }
   
   /**
    * Stores the current state before attempting to change the classloaders,
    * and the system properties.
    */
   private void stashInitialState()
   {
      if(trustStorePath.get() == null && trustStorePassword.get() == null)
      {
         trustStorePath.set(System.getProperty("javax.net.ssl.trustStore"));
         trustStorePassword.set(System.getProperty("javax.net.ssl.trustStorePassword"));
      }
   }
   
   /**
    * Unsets the thread's context classloader to the original classloader. 
    * We'll do this to ensure that Arquillian tests may run unaffected,
    * if the {@link WebLogicJMXLibClassLoader} were to interfere somehow.
    * 
    * The truststore path and password is also reset to the original,
    * to ensure that Arquillian tests at the client, that use these properties,
    * will run without interference.
    * 
    * This method is preferably invoked as soon as possible.
    */
   private void revertToInitialState()
   {
      if(trustStorePath.get() != null && trustStorePassword.get() != null)
      {
         System.setProperty("javax.net.ssl.trustStore", trustStorePath.get());
         System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword.get());
         trustStorePath.set(null);
         trustStorePassword.set(null);
      }
   }
   
   private void setupState()
   {
      if(configuration.isUseDemoTrust() || configuration.isUseCustomTrust() || configuration.isUseJavaStandardTrust())
      {
         System.setProperty("javax.net.ssl.trustStore", configuration.getTrustStoreLocation());
         String trustStorePassword = configuration.getTrustStorePassword();
         // The default password for JKS truststores 
         // usually need not be specified to read the CA certs.
         // But, if this was specified in arquillian.xml, we'll set it.
         if(trustStorePassword != null && !trustStorePassword.equals(""))
         {
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword); 
         }
      }
   }

}
