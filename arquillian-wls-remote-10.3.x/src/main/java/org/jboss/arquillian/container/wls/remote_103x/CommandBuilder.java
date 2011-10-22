package org.jboss.arquillian.container.wls.remote_103x;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder
{

   private String weblogicJarPath;
   private String adminUrl;
   private String adminUserName;
   private String adminPassword;
   private String deploymentName;
   private String targets;
   private String deploymentArchivePath;

   public CommandBuilder setWeblogicJarPath(String weblogicJarPath)
   {
      this.weblogicJarPath = weblogicJarPath;
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
   
   public List<String> buildDeployCommand()
   {
      //TODO: Validate that the builder is in the right state. All args to be validated.
      List<String> cmd = new ArrayList<String>();
      cmd.add("java");
      cmd.add("-classpath");
      cmd.add(weblogicJarPath);
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
      return cmd;
   }
   
   public List<String> buildUndeployCommand()
   {
      //TODO: Validate that the builder is in the right state. All args to be validated.
      List<String> cmd = new ArrayList<String>();
      cmd.add("java");
      cmd.add("-classpath");
      cmd.add(weblogicJarPath);
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
      return cmd;
   }
   
}
