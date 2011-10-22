package org.jboss.arquillian.container.wls.remote_103x;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;

/**
 * Utility class that uses Weblogic.Deployer to conduct deployments and undeployments.
 * This might need to be revisited, especially since WebLogic supports JSR-88 in some form.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicDeployerClient
{

   private static final Logger logger = Logger.getLogger(WebLogicDeployerClient.class.getName());
   private static final String ADMIN_URL_TEMPLATE = "%s://%s:%d";
   private static final String WEBLOGIC_JAR_PATH = "server/lib/weblogic.jar";
   
   private Process deployer;
   private WebLogicConfiguration configuration;

   public WebLogicDeployerClient(WebLogicConfiguration configuration)
   {
      this.configuration = configuration;
   }

   public void deploy(String deploymentName, File deploymentArchive) throws DeploymentException
   {
      String wlsHome = configuration.getWlsHome();
      String weblogicJarPath = wlsHome.endsWith(File.separator) ?  wlsHome.concat(WEBLOGIC_JAR_PATH) : wlsHome.concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      String adminUrl = String.format(ADMIN_URL_TEMPLATE, configuration.getProtocol(), configuration.getAdminListenAddress(), configuration.getAdminListenPort());
      
      CommandBuilder builder = new CommandBuilder()
            .setWeblogicJarPath(weblogicJarPath)
            .setAdminUrl(adminUrl)
            .setAdminUserName(configuration.getAdminUserName())
            .setAdminPassword(configuration.getAdminPassword())
            .setDeploymentName(deploymentName)
            .setDeploymentArchivePath(deploymentArchive.getAbsolutePath())
            .setTargets(configuration.getTarget());
      
      logger.log(Level.INFO, "Starting weblogic.Deployer to deploy the test artifact.");
      forkWebLogicDeployer(builder.buildDeployCommand());
   }

   public void undeploy(String deploymentName) throws DeploymentException
   {
      String wlsHome = configuration.getWlsHome();
      String weblogicJarPath = wlsHome.endsWith(File.separator) ?  wlsHome.concat(WEBLOGIC_JAR_PATH) : wlsHome.concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      String adminUrl = String.format(ADMIN_URL_TEMPLATE, configuration.getProtocol(), configuration.getAdminListenAddress(), configuration.getAdminListenPort());

      CommandBuilder builder = new CommandBuilder()
            .setWeblogicJarPath(weblogicJarPath)
            .setAdminUrl(adminUrl)
            .setAdminUserName(configuration.getAdminUserName())
            .setAdminPassword(configuration.getAdminPassword())
            .setDeploymentName(deploymentName)
            .setTargets(configuration.getTarget());
      
      logger.log(Level.INFO, "Starting weblogic.Deployer to undeploy the test artifact.");
      forkWebLogicDeployer(builder.buildUndeployCommand());
   }

   private void forkWebLogicDeployer(List<String> deployerCmd) throws DeploymentException
   {
      try
      {
         ProcessBuilder builder = new ProcessBuilder(deployerCmd);
         builder.redirectErrorStream(true);
         deployer = builder.start();
         Thread outputReader = new Thread(new DeployerOutputReader());
         outputReader.start();
         int exitValue = deployer.waitFor();
         logger.log(Level.INFO, "weblogic.Deployer terminated with exit code: {0}", exitValue);
      }
      catch (InterruptedException interruptEx)
      {
         throw new DeploymentException("The thread was interrupted.", interruptEx);
      }
      catch (IOException ioEx)
      {
         throw new DeploymentException("Failed to execute weblogic.Deployer", ioEx);
      }
   }

   class DeployerOutputReader implements Runnable
   {

      public void run()
      {
         InputStream is = deployer.getInputStream();
         BufferedReader reader = new BufferedReader(new InputStreamReader(is));
         String line = null;
         try
         {
            while((line = reader.readLine()) != null)
            {
               logger.log(Level.FINE, line);
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
      
   }
}