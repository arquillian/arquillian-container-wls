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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.wls.CommonWebLogicConfiguration;

/**
 * Utility class that uses Weblogic.Deployer to conduct deployments and undeployments.
 * 
 * The use of this might need to be revisited, especially since WebLogic supports JSR-88 in some form.
 * If the JSR-88 support can be used to deploy apps with the Java Management API and not
 * required any proprietary classes, then we must look at replacing this class.
 * 
 * The output of the weblogic.Deployer process is only displayed and not parsed.
 * We'll use the JMX client to actually figure out the details of the deployment.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicDeployerClient
{

   private static final Logger logger = Logger.getLogger(WebLogicDeployerClient.class.getName());
   
   private Process deployer;
   private CommonWebLogicConfiguration configuration;
   private StringBuilder buffer;

   public WebLogicDeployerClient(CommonWebLogicConfiguration configuration)
   {
      this.configuration = configuration;
   }

   /**
    * Forks the weblogic.Deployer process to trigger a deployment.
    * 
    * This is more or less a fire and forget method. Exceptions
    * thrown by this method are generally indicative of a failed deployment.
    * But this is not necessarily so - the caller must also verify the status
    * of the deployment with the AdminServer, via JMX or other means.
    *  
    * @param deploymentName The name of the application to be deployed
    * @param deploymentArchive The file archive (EAR/WAR) representing the application
    * 
    * @throws DeploymentException When forking of weblogic.Deployer fails,
    * or when interaction with the forked process fails.
    */
   public void deploy(String deploymentName, File deploymentArchive) throws DeploymentException
   {
      CommandBuilder builder = new CommandBuilder()
            .setClassPath(configuration.getClassPath())
            .setAdminUrl(configuration.getAdminUrl())
            .setAdminUserName(configuration.getAdminUserName())
            .setAdminPassword(configuration.getAdminPassword())
            .setDeploymentName(deploymentName)
            .setDeploymentArchivePath(deploymentArchive.getAbsolutePath())
            .setTargets(configuration.getTarget())
            .setUseDemoTrust(configuration.isUseDemoTrust())
            .setUseCustomTrust(configuration.isUseCustomTrust())
            .setCustomTrustStore(configuration.getTrustStoreLocation())
            .setUseJavaStandardTrust(configuration.isUseJavaStandardTrust())
            .setIgnoreHostNameVerification(configuration.isIgnoreHostNameVerification())
            .setHostnameVerifierClass(configuration.getHostnameVerifierClass())
            .setUseURandom(configuration.isUseURandom())
            .setRemoteMachine(configuration.isRemoteMachine());
      
      logger.log(Level.INFO, "Starting weblogic.Deployer to deploy the test artifact.");
      forkWebLogicDeployer(builder.buildDeployCommand());
   }

   /**
    * Forks the weblogic.Deployer process to trigger an undeployment.
    * 
    * @param deploymentName The name of the application to be undeployed
    * 
    * @throws DeploymentException When forking of weblogic.Deployer fails,
    * or when interaction with the forked process fails.
    */
   public void undeploy(String deploymentName) throws DeploymentException
   {
      CommandBuilder builder = new CommandBuilder()
            .setClassPath(configuration.getClassPath())
            .setAdminUrl(configuration.getAdminUrl())
            .setAdminUserName(configuration.getAdminUserName())
            .setAdminPassword(configuration.getAdminPassword())
            .setDeploymentName(deploymentName)
            .setTargets(configuration.getTarget())
            .setUseDemoTrust(configuration.isUseDemoTrust())
            .setUseCustomTrust(configuration.isUseCustomTrust())
            .setCustomTrustStore(configuration.getTrustStoreLocation())
            .setUseJavaStandardTrust(configuration.isUseJavaStandardTrust())
            .setIgnoreHostNameVerification(configuration.isIgnoreHostNameVerification())
            .setHostnameVerifierClass(configuration.getHostnameVerifierClass())
            .setUseURandom(configuration.isUseURandom());
      
      logger.log(Level.INFO, "Starting weblogic.Deployer to undeploy the test artifact.");
      forkWebLogicDeployer(builder.buildUndeployCommand());
   }

   private void forkWebLogicDeployer(List<String> deployerCmd) throws DeploymentException
   {
      try
      {
         buffer = new StringBuilder();
         ProcessBuilder builder = new ProcessBuilder(deployerCmd);
         builder.redirectErrorStream(true);
         deployer = builder.start();
         Thread outputReader = new Thread(new DeployerOutputReader());
         outputReader.start();
         int exitValue = deployer.waitFor();
         // We'll not throw an error yet, as we do not want to parse the output of weblogic.Deployer
         // to determine if the deployment failed. So, we'll log the process exit value,
         // and defer the evaluation of the deployment status to the JMX client.
         if(exitValue == 0)
         {
            logger.log(Level.INFO, "weblogic.Deployer appears to have terminated successfully.");
         }
         else
         {
            logger.log(Level.WARNING, "weblogic.Deployer terminated abnormally with exit code {0}", exitValue);
            logger.log(Level.INFO, "The output of the weblogic.Deployer process was:\n {0}", buffer.toString());
         }
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

   /**
    * Reads the output stream of the weblogic.Deployer process
    * and logs it.
    * 
    * The logger level is set to FINE as we do not want
    * to display these details to the user by default. This would
    * make for cleaner JUnit/TestNG generated logs/reports.  
    * 
    * @author Vineet Reynolds
    *
    */
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
               // Store the output anyway, so that it may be logged later,
               // in the same Arquillian test run, if weblogic.Deployer terminates abruptly.
               // Used for developer convenience, as failures may be abrupt and we do not want anyone to rerun tests.
               buffer.append(line);
               buffer.append('\n');
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }
      
   }
}