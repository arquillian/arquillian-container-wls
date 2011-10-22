package org.jboss.arquillian.container.wls.remote_103x;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that uses Weblogic.Deployer to conduct deployments and undeployments.
 * This might need to be revisited, especially since WebLogic supports JSR-88 is some form.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicDeployerClient
{

   private Process deployer;
   private WebLogicConfiguration configuration;
   private static final String ADMIN_URL_TEMPLATE = "%s://%s:%d";
   private static final String WEBLOGIC_JAR_PATH = "server/lib/weblogic.jar";

   public WebLogicDeployerClient(WebLogicConfiguration configuration)
   {
      this.configuration = configuration;
   }

   public void deploy(String deploymentName, File deploymentArchive)
   {
      String wlsHome = configuration.getWlsHome();
      String weblogicJarPath = wlsHome.endsWith(File.separator) ?  wlsHome.concat(WEBLOGIC_JAR_PATH) : wlsHome.concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      System.out.println("Starting deployer");
      List<String> cmd = new ArrayList<String>();
      cmd.add("java");
      cmd.add("-classpath");
      cmd.add(weblogicJarPath);
      cmd.add("weblogic.Deployer");
      cmd.add("-adminurl");
      cmd.add(String.format(ADMIN_URL_TEMPLATE, configuration.getProtocol(), configuration.getAdminListenAddress(), configuration.getAdminListenPort()));
      cmd.add("-username");
      cmd.add(configuration.getAdminUserName());
      cmd.add("-password");
      cmd.add(configuration.getAdminPassword());
      cmd.add("-deploy");
      cmd.add("-name");
      cmd.add(deploymentName);
      cmd.add("-source");
      cmd.add(deploymentArchive.getAbsolutePath());
      cmd.add("-targets");
      cmd.add(configuration.getTarget());
      cmd.add("-upload");
      
      try
      {
         ProcessBuilder builder = new ProcessBuilder(cmd);
         builder.redirectErrorStream(true);
         deployer = builder.start();
         Thread outputReader = new Thread(new DeployerOutputReader());
         outputReader.start();
         outputReader.join();
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }
   
   public void undeploy(String deploymentName)
   {
      String wlsHome = configuration.getWlsHome();
      String weblogicJarPath = wlsHome.endsWith(File.separator) ?  wlsHome.concat(WEBLOGIC_JAR_PATH) : wlsHome.concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      System.out.println("Starting deployer");
      List<String> cmd = new ArrayList<String>();
      cmd.add("java");
      cmd.add("-classpath");
      cmd.add(weblogicJarPath);
      cmd.add("weblogic.Deployer");
      cmd.add("-adminurl");
      cmd.add(String.format(ADMIN_URL_TEMPLATE, configuration.getProtocol(), configuration.getAdminListenAddress(), configuration.getAdminListenPort()));
      cmd.add("-username");
      cmd.add(configuration.getAdminUserName());
      cmd.add("-password");
      cmd.add(configuration.getAdminPassword());
      cmd.add("-undeploy");
      cmd.add("-name");
      cmd.add(deploymentName);
      cmd.add("-targets");
      cmd.add(configuration.getTarget());
     
      try
      {
         ProcessBuilder builder = new ProcessBuilder(cmd);
         builder.redirectErrorStream(true);
         deployer = builder.start();
         Thread outputReader = new Thread(new DeployerOutputReader());
         outputReader.start();
         outputReader.join();
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
      catch (IOException e)
      {
         e.printStackTrace();
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
               System.out.println(line);
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
      
   }
}


