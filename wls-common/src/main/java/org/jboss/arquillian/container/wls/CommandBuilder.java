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
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the commandline arguments to launch the weblogic.Deployer process.
 * Implements the Builder pattern.
 * 
 * @author Vineet Reynolds
 */
public class CommandBuilder
{

   private String classPath;
   private String adminUrl;
   private String adminUserName;
   private String adminPassword;
   private String deploymentName;
   private String targets;
   private String deploymentArchivePath;
   private boolean useDemoTrust;
   private boolean useCustomTrust;
   private String customTrustStore;
   private boolean useJavaStandardTrust;
   private String trustStorePassword;
   private boolean ignoreHostNameVerification;
   private String hostnameVerifierClass;
   private boolean useURandom;
   private boolean remoteMachine;

   public CommandBuilder setClassPath(String classPath)
   {
      this.classPath = classPath;
      return this;
   }

   public CommandBuilder setAdminUrl(String adminUrl)
   {
      this.adminUrl = adminUrl;
      return this;
   }

   public CommandBuilder setAdminUserName(String adminUserName)
   {
      this.adminUserName = adminUserName;
      return this;
   }

   public CommandBuilder setAdminPassword(String adminPassword)
   {
      this.adminPassword = adminPassword;
      return this;
   }

   public CommandBuilder setDeploymentName(String deploymentName)
   {
      this.deploymentName = deploymentName;
      return this;
   }

   public CommandBuilder setTargets(String targets)
   {
      this.targets = targets;
      return this;
   }

   public CommandBuilder setDeploymentArchivePath(String deploymentArchivePath)
   {
      this.deploymentArchivePath = deploymentArchivePath;
      return this;
   }

   public CommandBuilder setUseDemoTrust(boolean useDemoTrust)
   {
      this.useDemoTrust = useDemoTrust;
      return this;
   }
   
   public CommandBuilder setUseCustomTrust(boolean useCustomTrust)
   {
      this.useCustomTrust = useCustomTrust;
      return this;
   }
   
   public CommandBuilder setUseJavaStandardTrust(boolean useJavaStandardTrust)
   {
      this.useJavaStandardTrust = useJavaStandardTrust;
      return this;
   }
   
   public CommandBuilder setCustomTrustStore(String customTrustStore)
   {
      this.customTrustStore = customTrustStore;
      return this;
   }

   public CommandBuilder setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword = trustStorePassword;
      return this;
   }
   
   public CommandBuilder setIgnoreHostNameVerification(boolean ignoreHostNameVerification)
   {
      this.ignoreHostNameVerification = ignoreHostNameVerification;
      return this;
   }

   public CommandBuilder setHostnameVerifierClass(String hostnameVerifierClass)
   {
      this.hostnameVerifierClass = hostnameVerifierClass;
      return this;
   }
   
   public CommandBuilder setUseURandom(boolean useURandom)
   {
      this.useURandom = useURandom;
      return this;
   }

   public CommandBuilder setRemoteMachine(boolean remoteMachine) {
      this.remoteMachine = remoteMachine;
      return this;
   }

  /**
    * Constructs the commandline to be used for launching weblogic.Deployer
    * to deploy an app.
    * 
    * @return A {@link List} of {@link String} that contains the commandline
    *  to be used to launch weblogic.Deployer for deploying an app.
    */
   public List<String> buildDeployCommand()
   {
      List<String> cmd = new ArrayList<String>();
      // Use the same Java Home as the one used by Arquillian to launch weblogic.Deployer.
      // This will avoid confusion over the cacerts file if used as the SSL Trust Store.
      cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
      cmd.add("-classpath");
      cmd.add(classPath);
      if(useDemoTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=DemoTrust");
      }
      if(useCustomTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=CustomTrust");
         cmd.add("-Dweblogic.security.CustomTrustKeyStoreFileName="+customTrustStore);
         cmd.add("-Dweblogic.security.TrustKeystoreType=jks");
         if(trustStorePassword != null && !trustStorePassword.equals(""))
         {
            cmd.add("-Dweblogic.security.CustomTrustKeyStorePassPhrase="+trustStorePassword);
         }
      }
      if(useJavaStandardTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=JavaStandardTrust");
         if(trustStorePassword != null && !trustStorePassword.equals(""))
         {
            cmd.add("-Dweblogic.security.JavaStandardTrustKeyStorePassPhrase="+trustStorePassword);
         }
      }
      if(ignoreHostNameVerification)
      {
         cmd.add("-Dweblogic.security.SSL.ignoreHostnameVerification=true");
      }
      if(hostnameVerifierClass != null && !hostnameVerifierClass.equals(""))
      {
         cmd.add("-Dweblogic.security.SSL.hostnameVerifier=" + hostnameVerifierClass);
      }
      if(useURandom)
      {
          cmd.add("-Djava.security.egd=file:/dev/./urandom");
      }
      cmd.add("weblogic.Deployer");
      cmd.add("-adminurl");
      cmd.add(adminUrl);
      cmd.add("-username");
      cmd.add(adminUserName);
      cmd.add("-password");
      cmd.add(adminPassword);
      cmd.add("-deploy");
      cmd.add("-name");
      cmd.add(deploymentName);
      cmd.add("-source");
      cmd.add(deploymentArchivePath);
      cmd.add("-targets");
      cmd.add(targets);
      cmd.add("-upload");
      cmd.add("-debug");
      if (remoteMachine) {
         cmd.add("-remote");
      }
      return cmd;
   }
   
   /**
    * Constructs the commandline to be used for launching weblogic.Deployer
    * to undeploy an app.
    * 
    * @return A {@link List} of {@link String} that contains the commandline
    *  to be used to launch weblogic.Deployer for undeploying an app.
    */
   public List<String> buildUndeployCommand()
   {
      List<String> cmd = new ArrayList<String>();
      // Use the same Java Home as the one used by Arquillian to launch weblogic.Deployer.
      // This will avoid confusion over the cacerts file if used as the SSL Trust Store.
      cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
      cmd.add("-classpath");
      cmd.add(classPath);
      if(useDemoTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=DemoTrust");
      }
      if(useCustomTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=CustomTrust");
         cmd.add("-Dweblogic.security.CustomTrustKeyStoreFileName="+customTrustStore);
         cmd.add("-Dweblogic.security.TrustKeystoreType=jks");
         if(trustStorePassword != null && !trustStorePassword.equals(""))
         {
            cmd.add("-Dweblogic.security.CustomTrustKeyStorePassPhrase="+trustStorePassword);
         }
      }
      if(useJavaStandardTrust)
      {
         cmd.add("-Dweblogic.security.TrustKeyStore=JavaStandardTrust");
         if(trustStorePassword != null && !trustStorePassword.equals(""))
         {
            cmd.add("-Dweblogic.security.JavaStandardTrustKeyStorePassPhrase="+trustStorePassword);
         }
      }
      if(ignoreHostNameVerification)
      {
         cmd.add("-Dweblogic.security.SSL.ignoreHostnameVerification=true");
      }
      if(hostnameVerifierClass != null && !hostnameVerifierClass.equals(""))
      {
         cmd.add("-Dweblogic.security.SSL.hostnameVerifier=" + hostnameVerifierClass);
      }
      if(useURandom)
      {
          cmd.add("-Djava.security.egd=file:/dev/./urandom");
      }
      cmd.add("weblogic.Deployer");
      cmd.add("-adminurl");
      cmd.add(adminUrl);
      cmd.add("-username");
      cmd.add(adminUserName);
      cmd.add("-password");
      cmd.add(adminPassword);
      cmd.add("-undeploy");
      cmd.add("-name");
      cmd.add(deploymentName);
      cmd.add("-targets");
      cmd.add(targets);
      cmd.add("-debug");
      return cmd;
   }

}
