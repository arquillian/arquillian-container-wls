package org.jboss.arquillian.container.wls.remote_103x;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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

public class WebLogicJMXClient
{

   private static final Logger logger = Logger.getLogger(WebLogicJMXClient.class.getName());
   
   private static final String WL_JMX_CLIENT_JAR_PATH = "server/lib/wljmxclient.jar";

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
      catch (MalformedObjectNameException e)
      {
         throw new AssertionError(e.getMessage());
      }
   }

   public ProtocolMetaData deploy(String deploymentName, File deploymentArchive) throws DeploymentException
   {
      initWebLogicJMXLibClassLoader();
      createConnection();
      
      ProtocolMetaData metadata = new ProtocolMetaData();
      HTTPContext context = getTargetContextInfo();
      deployArchive(deploymentName, deploymentArchive);
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
   
   public void undeploy(String deploymentName) throws DeploymentException
   {
      undeployArchive(deploymentName);
      //TODO: We would want to verify whether the MBeanServer also records a successful undeployment.
   }


   private void deployArchive(String deploymentName, File deploymentArchive) throws DeploymentException
   {
      WebLogicDeployerClient deployerClient = new WebLogicDeployerClient(configuration);
      deployerClient.deploy(deploymentName, deploymentArchive);
   }

   private void undeployArchive(String deploymentName)
   {
      WebLogicDeployerClient deployerClient = new WebLogicDeployerClient(configuration);
      deployerClient.undeploy(deploymentName);
   }
   
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
                  throw new DeploymentException("The designated target server is not in the RUNNING state.");
               }
               
               String httpUrlAsString = (String) connection.invoke(wlServerRuntime, "getURL", new Object[]
               {"http"}, new String[]
               {"java.lang.String"});
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
         		"1. The target specified in arquillian.xml is not a valid WebLogic Server deployment target.");
      }
      return httpContext;
   }
   
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
      String wlsHome = configuration.getWlsHome();
      boolean separatorTerminated = wlsHome.endsWith(File.separator);
      String jmxLibPath = separatorTerminated ? wlsHome.concat(WL_JMX_CLIENT_JAR_PATH) : wlsHome.concat(File.separator).concat(WL_JMX_CLIENT_JAR_PATH);
      File wlHome =  new File(jmxLibPath);
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
   
   /*
   * Initialize connection to the Domain Runtime MBean Server
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
   
   private ObjectName[] getWLServerRuntimes() throws Exception
   {
      return (ObjectName[]) connection.getAttribute(domainRuntimeService, "ServerRuntimes");
   }

}
