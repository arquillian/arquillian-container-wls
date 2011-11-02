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
package org.jboss.arquillian.container.wls.remote_10_3;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

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
   private static final String WEBLOGIC_JAR_PATH = "server/lib/weblogic.jar";
   
   /**
    * The administration URL to connect to.
    */
   private String adminUrl;
   
   /**
    * Protocol to use to connect to AdminServer.
    * This is optional. It can be derived from the adminUrl. 
    */
   private String adminProtocol;

   /**
    * The listen address of the admin server.
    * This is optional. It can be derived from the adminUrl.
    */
   private String adminListenAddress;

   /**
    * The port of the admin server.
    * This is optional. It can be derived from the adminUrl.
    */
   private int adminListenPort;
   
   /**
    * The name of the Administrator user.
    */
   private String adminUserName;
   
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
    */
   private String target;
   
   /**
    * The location of weblogic.jar (optional)
    */
   private String weblogicJarPath;
   
   /**
    * The location of the wljmxclient.jar (optional)
    */
   private String jmxLibPath;
   
   /**
    * The protocol to use, when connecting to the WebLogic Domain Runtime MBean Server. (optional)
    */
   private String jmxProtocol;
   
   /**
    * The host where the WebLogic Domain Runtime MBean Server resides. (optional)
    */
   private String jmxHost;
   
   /**
    * The port that the WebLogic Domain Runtime MBean Server listens on. (optional)
    */
   private int jmxPort;
   
   /**
    * Use the Demo Truststore, to connect to a WebLogic Server that uses Demo Identity and Trust stores.
    */
   private boolean useDemoTrust = false;

   /**
    * Use a custom Truststore, to connect to a WebLogic Server that uses a Custom Trust store.
    */
   private boolean useCustomTrust = false;

   /**
    * Use a the Truststore of the running JRE, to connect to a WebLogic Server that uses the Java Standard Trust store.
    */
   private boolean useJavaStandardTrust = false;
   
   private String javaHome;
   
   private String trustStoreLocation;
   
   private String trustStorePassword;
   
   public void validate() throws ConfigurationException
   {
      // Verify the mandatory properties
      Validate.directoryExists(wlsHome,
            "The wlsHome directory resolved to " + wlsHome + " and could not be located. Verify the property in arquillian.xml");
      Validate.notNullOrEmpty(adminUrl, "The adminUrl is empty. Verify the property in arquillian.xml");
      Validate.notNullOrEmpty(adminUserName,
            "The username provided to weblogic.Deployer is empty. Verify the credentials in arquillian.xml");
      Validate.notNullOrEmpty(adminPassword,
            "The password provided to weblogic.Deployer is empty. Verify the credentials in arquillian.xml");
      Validate
            .notNullOrEmpty(target, "The target for the deployment is empty. Verify the properties in arquillian.xml");

      // Once validated, set the properties that can be derived, if not already set.
      try
      {
         URI adminURI = new URI(adminUrl);
         adminProtocol = adminURI.getScheme();
         adminListenAddress = adminURI.getHost();
         adminListenPort = adminURI.getPort();
         Validate.notNullOrEmpty(adminProtocol, "The adminProtocol is empty. Verify the adminUrl and adminProtocol properties in arquillian.xml");
         Validate.isInList(adminProtocol, new String[]
               {"t3", "t3s", "http", "https", "iiop", "iiops"},
               "The adminProtocol is invalid. It must be either t3, t3s, http, https, iiop or iiops.");
         Validate.notNullOrEmpty(adminListenAddress,
               "The adminListenAddress is empty. Verify the adminUrl and adminListenAddress properties in arquillian.xml");
         Validate.isInRange(adminListenPort, 0, 65535,
               "The adminListenPort is invalid. Verify the adminUrl and adminListenPort properties in arquillian.xml");
      }
      catch (URISyntaxException uriEx)
      {
         throw new IllegalArgumentException("Failed to parse the adminUrl property - " + adminUrl + " as a URI.",uriEx);
      }
      
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
      if((jmxProtocol == null || jmxProtocol.equals("")) && (jmxHost == null || jmxHost.equals("")))
      {
         jmxProtocol = adminProtocol;
         jmxHost = adminListenAddress;
         jmxPort = adminListenPort;
      }
      
      Boolean[] trustTypes = new Boolean[]
      {useDemoTrust, useCustomTrust, useJavaStandardTrust};
      int specifiedTrustType = 0;
      for (int ctr = 0; ctr < trustTypes.length; ctr++)
      {
         if (trustTypes[ctr])
         {
            specifiedTrustType++;
         }
      }
      if (specifiedTrustType > 1)
      {
         throw new IllegalArgumentException(
               "Only one of useDemoTrust, useCustomTrust and useJavaStandardTrust must be specified as true. Verify these properties in arquillian.xml ");
      }
      
      if (useDemoTrust)
      {
         trustStoreLocation = wlsHome.endsWith(File.separator) ? wlsHome.concat("server/lib/DemoTrust.jks") : wlsHome
               .concat(File.separator).concat("server/lib/DemoTrust.jks");
         Validate.isValidFile(trustStoreLocation, "The DemoTrust.jks file was resolved to " + trustStoreLocation
               + " and could not be located. Verify the wlsHome and useDemoTrust properties in arquillian.xml");
      }
      if(useCustomTrust)
      {
         Validate.isValidFile(trustStoreLocation, "The trustStoreLocation file was resolved to " + trustStoreLocation
               + " and could not be located. Verify the useCustomTrust and trustStoreLocation properties in arquillian.xml");
      }
      if(useJavaStandardTrust)
      {
         trustStoreLocation = System.getProperty("java.home") + File.separator + "lib" + File.separator + "security"
               + File.separator + "cacerts";
         Validate.isValidFile(trustStoreLocation, "The trustStoreLocation file was resolved to " + trustStoreLocation
               + " and could not be located. Verify that the cacerts file is present in the JRE installation.");
      }
      
      //Validate these derived properties
      Validate.isValidFile(jmxLibPath,
            "The wljmxclient.jar could not be located. Verify the wlsHome and jmxLibPath properties in arquillian.xml");
      Validate
            .isValidFile(weblogicJarPath,
                  "The weblogic.jar could not be located. Verify the wlsHome and weblogicJarPath properties in arquillian.xml");
      Validate
            .notNullOrEmpty(jmxProtocol,
                  "The jmxProtocol is empty. Verify the adminUrl, adminProtocol and jmxProtocol properties in arquillian.xml");
      Validate.isInList(jmxProtocol, new String[]
            {"t3", "t3s", "http", "https", "iiop", "iiops"},
            "The jmxProtocol is invalid. It must be either t3, t3s, http, https, iiop or iiops.");
      Validate.notNullOrEmpty(jmxHost,
            "The jmxHost is empty. Verify the adminUrl, adminListenAddress and jmxHost properties in arquillian.xml");
      Validate.isInRange(jmxPort, 0, 65535,
            "The jmxPort is invalid. Verify the adminUrl, adminListenPort and jmxPort properties in arquillian.xml");
   }

   public String getAdminUrl()
   {
      return adminUrl;
   }

   public void setAdminUrl(String adminUrl)
   {
      this.adminUrl = adminUrl;
   }

   public String getAdminProtocol()
   {
      return adminProtocol;
   }

   public void setAdminProtocol(String adminProtocol)
   {
      this.adminProtocol = adminProtocol;
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
   
   public String getJmxProtocol()
   {
      return jmxProtocol;
   }

   public void setJmxProtocol(String jmxProtocol)
   {
      this.jmxProtocol = jmxProtocol;
   }

   public String getJmxHost()
   {
      return jmxHost;
   }

   public void setJmxHost(String jmxHost)
   {
      this.jmxHost = jmxHost;
   }

   public int getJmxPort()
   {
      return jmxPort;
   }

   public void setJmxPort(int jmxPort)
   {
      this.jmxPort = jmxPort;
   }

   public boolean isUseDemoTrust()
   {
      return useDemoTrust;
   }

   public void setUseDemoTrust(boolean useDemoTrust)
   {
      this.useDemoTrust = useDemoTrust;
   }

   public boolean isUseCustomTrust()
   {
      return useCustomTrust;
   }

   public void setUseCustomTrust(boolean useCustomTrust)
   {
      this.useCustomTrust = useCustomTrust;
   }

   public boolean isUseJavaStandardTrust()
   {
      return useJavaStandardTrust;
   }

   public void setUseJavaStandardTrust(boolean useJavaStandardTrust)
   {
      this.useJavaStandardTrust = useJavaStandardTrust;
   }

   public String getJavaHome()
   {
      return javaHome;
   }

   public void setJavaHome(String javaHome)
   {
      this.javaHome = javaHome;
   }

   public String getTrustStoreLocation()
   {
      return trustStoreLocation;
   }

   public void setTrustStoreLocation(String trustStoreLocation)
   {
      this.trustStoreLocation = trustStoreLocation;
   }

   public String getTrustStorePassword()
   {
      return trustStorePassword;
   }

   public void setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword = trustStorePassword;
   }
   
}
