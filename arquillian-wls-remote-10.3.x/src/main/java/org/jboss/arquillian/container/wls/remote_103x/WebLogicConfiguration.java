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

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * The Arquillian properties for the WebLogic 10.3.x container.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicConfiguration implements ContainerConfiguration
{

   private static final String WL_JMX_CLIENT_JAR_PATH = "server/lib/wljmxclient.jar";
   private static final String ADMIN_URL_TEMPLATE = "%s://%s:%d";
   private static final String WEBLOGIC_JAR_PATH = "server/lib/weblogic.jar";
   
   /**
    * Protocol to use to connect to AdminServer, used to construct the adminurl.
    * Valid ones are t3, http, iiop, iiops.
    */
   private String protocol = "t3";

   /**
    * The listen address of the admin server, that is used to construct the adminurl.
    */
   private String adminListenAddress = "localhost";

   /**
    * The port of the admin server, that is used to construct the adminurl.
    */
   private int adminListenPort = 7001;
   
   /**
    * The name of the Administrator user.
    */
   private String adminUserName = "weblogic";
   
   /**
    * The password of the Administrator user.
    */
   private String adminPassword;
   
   /**
    * The location of the local WebLogic Server installation.
    * The parent directory of this location is usually named wlserver_10.3.
    * The directory must also contain the 'common' and 'server' subdirectories.
    */
   private String wlsHome;
   
   /**
    * The name of the target for the deployment.
    * This can be the name of the Admin Server i.e. "AdminServer",
    * the name of an individual Managed Server or the name of a Cluster (not yet supported).  
    * 
    * The default is "AdminServer", but this is not recommended
    * for simulating a production environment during integration testing. 
    */
   private String target = "AdminServer";
   
   /**
    * The location of weblogic.jar (optional)
    */
   private String weblogicJarPath;
   
   /**
    * The location of the wljmxclient.jar (optional)
    */
   private String jmxLibPath;
   
   /**
    * The administration URL to connect to. (optional)
    */
   private String adminUrl;
   
   public void validate() throws ConfigurationException
   {
      // Verify the mandatory properties
      Validate.directoryExists(wlsHome,
            "The wlsHome directory could not be located. Verify the property in arquillian.xml");
      Validate.notNullOrEmpty(protocol, "The protocol is empty. Verify the property in arquillian.xml");
      Validate.notNullOrEmpty(adminListenAddress,
            "The adminListenAddress is empty. Verify the property in arquillian.xml");
      Validate.isInRange(adminListenPort, 0, 65535,
            "The adminListenPort is invalid. Verify the property in arquillian.xml");
      Validate.notNullOrEmpty(adminUserName,
            "The username provided to weblogic.Deployer is empty. Verify the credentials in arquillian.xml");
      Validate.notNullOrEmpty(adminPassword,
            "The password provided to weblogic.Deployer is empty. Verify the credentials in arquillian.xml");
      Validate
            .notNullOrEmpty(target, "The target for the deployment is empty. Verify the properties in arquillian.xml");

      // Once validated, set the admin URL, jmxLibPath and weblogicJarPath if not already set.
      if (jmxLibPath == null || jmxLibPath.equals(""))
      {
         this.jmxLibPath = this.wlsHome.endsWith(File.separator) ? wlsHome.concat(WL_JMX_CLIENT_JAR_PATH) : wlsHome
               .concat(File.separator).concat(WL_JMX_CLIENT_JAR_PATH);
      }
      if (weblogicJarPath == null || weblogicJarPath.equals(""))
      {
         this.weblogicJarPath = this.wlsHome.endsWith(File.separator) ? wlsHome.concat(WEBLOGIC_JAR_PATH) : wlsHome
               .concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      }
      if (adminUrl == null || adminUrl.equals(""))
      {
         adminUrl = String.format(ADMIN_URL_TEMPLATE, this.protocol, this.adminListenAddress, this.adminListenPort);
      }
      
      //Validate these optional properties
      Validate.isValidFile(jmxLibPath,
            "The wljmxclient.jar could not be located. Verify the wlsHome and jmxLibPath properties in arquillian.xml");
      Validate
            .isValidFile(weblogicJarPath,
                  "The weblogic.jar could not be located. Verify the wlsHome and weblogicJarPath properties in arquillian.xml");
      Validate
            .notNullOrEmpty(
                  adminUrl,
                  "The adminUrl is empty. Verify the adminListenAddress, adminListenPort, protocol and adminUrl properties in arquillian.xml");
   }

   public String getProtocol()
   {
      return protocol;
   }

   public void setProtocol(String protocol)
   {
      this.protocol = protocol;
   }

   public String getAdminListenAddress()
   {
      return adminListenAddress;
   }

   public void setAdminListenAddress(String adminListenAddress)
   {
      this.adminListenAddress = adminListenAddress;
   }

   public int getAdminListenPort()
   {
      return adminListenPort;
   }

   public void setAdminListenPort(int adminListenPort)
   {
      this.adminListenPort = adminListenPort;
   }

   public String getAdminUserName()
   {
      return adminUserName;
   }

   public void setAdminUserName(String adminUserName)
   {
      this.adminUserName = adminUserName;
   }

   public String getAdminPassword()
   {
      return adminPassword;
   }

   public void setAdminPassword(String adminPassword)
   {
      this.adminPassword = adminPassword;
   }

   public String getWlsHome()
   {
      return wlsHome;
   }

   public void setWlsHome(String wlsHome)
   {
      this.wlsHome = wlsHome;
   }

   public String getTarget()
   {
      return target;
   }

   public void setTarget(String target)
   {
      this.target = target;
   }

   public String getWeblogicJarPath()
   {
      return weblogicJarPath;
   }

   public void setWeblogicJarPath(String weblogicJarPath)
   {
      this.weblogicJarPath = weblogicJarPath;
   }

   public String getJmxLibPath()
   {
      return jmxLibPath;
   }

   public void setJmxLibPath(String jmxLibPath)
   {
      this.jmxLibPath = jmxLibPath;
   }

   public String getAdminUrl()
   {
      return adminUrl;
   }

   public void setAdminUrl(String adminUrl)
   {
      this.adminUrl = adminUrl;
   }

}
