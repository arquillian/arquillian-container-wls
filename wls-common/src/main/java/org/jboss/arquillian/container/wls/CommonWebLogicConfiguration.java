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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

/**
 * The Arquillian properties that are common across WebLogic containers.
 * 
 * @author Vineet Reynolds
 *
 */
public class CommonWebLogicConfiguration implements ContainerConfiguration
{
   private static final Logger logger = Logger.getLogger(CommonWebLogicConfiguration.class.getName());
   private static final String WEBLOGIC_JAR_PATH = "server/lib/weblogic.jar";
   private static final String JMX_CLIENT_JAR_PATH = "server/lib/wljmxclient.jar";
   
   private String adminUrl;
   
   private String adminProtocol;

   private String adminListenAddress;

   private int adminListenPort;
   
   private String adminUserName;
   
   private String adminPassword;
   
   private String wlsHome;
   
   private String wlHome = System.getenv("WL_HOME");
   
   private String target;
   
   private String weblogicJarPath;
   
   private String jmxClientJarPath;
   
   private String jmxProtocol;
   
   private String jmxHost;
   
   private int jmxPort;
   
   private boolean useDemoTrust = false;

   private boolean useCustomTrust = false;

   private boolean useJavaStandardTrust = false;
   
   private String trustStoreLocation;
   
   private String trustStorePassword;
   
   private boolean ignoreHostNameVerification;
   
   private String hostnameVerifierClass;
   
   private String classPath;
   
   private boolean useURandom;
   
   public void validate() throws ConfigurationException
   {
      // Verify the mandatory properties
      if(wlsHome != null && wlsHome.length() > 0)
      {
         Validate.directoryExists(wlsHome,
            "The wlsHome directory resolved to " + wlsHome + " and could not be located. Verify the property in arquillian.xml");
         logger.log(Level.WARNING, "The wlsHome property is deprecated. Use the wlHome property instead.");
         wlHome = wlsHome;
      }
      Validate.directoryExists(wlHome,
              "The wlHome directory resolved to " + wlHome + " and could not be located. Verify the property in arquillian.xml");
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
      
      if (weblogicJarPath == null || weblogicJarPath.equals(""))
      {
         this.weblogicJarPath = this.wlHome.endsWith(File.separator) ? wlHome.concat(WEBLOGIC_JAR_PATH) : wlHome
               .concat(File.separator).concat(WEBLOGIC_JAR_PATH);
      }
      if (jmxClientJarPath == null || jmxClientJarPath.equals(""))
      {
         this.jmxClientJarPath = this.wlHome.endsWith(File.separator) ? wlHome.concat(JMX_CLIENT_JAR_PATH) : wlHome
               .concat(File.separator).concat(JMX_CLIENT_JAR_PATH);
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
         trustStoreLocation = wlHome.endsWith(File.separator) ? wlHome.concat("server/lib/DemoTrust.jks") : wlHome
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
      
      // Set the classpath for weblogic.Deployer
      Validate
      .isValidFile(weblogicJarPath,
            "The weblogic.jar could not be located. Verify the wlsHome and weblogicJarPath properties in arquillian.xml");
      if(classPath != null && !classPath.equals(""))
      {
         classPath = weblogicJarPath + File.pathSeparator + classPath;
      }
      else
      {
         classPath = weblogicJarPath;
      }
      
      //Validate these derived properties
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

   /**
    * @param adminUrl The administration URL to connect to.
    */
   public void setAdminUrl(String adminUrl)
   {
      this.adminUrl = adminUrl;
   }

   public String getAdminProtocol()
   {
      return adminProtocol;
   }

    /**
     * @param adminProtocol Protocol to use to connect to AdminServer. This is optional. It can be derived from the adminUrl.
     */
   public void setAdminProtocol(String adminProtocol)
   {
      this.adminProtocol = adminProtocol;
   }

   public String getAdminListenAddress()
   {
      return adminListenAddress;
   }

    /**
     * @param adminListenAddress The listen address of the admin server. This is optional. It can be derived from the adminUrl.
     */
   public void setAdminListenAddress(String adminListenAddress)
   {
      this.adminListenAddress = adminListenAddress;
   }

   public int getAdminListenPort()
   {
      return adminListenPort;
   }

    /**
     * @param adminListenPort The port of the admin server. This is optional. It can be derived from the adminUrl.
     */
   public void setAdminListenPort(int adminListenPort)
   {
      this.adminListenPort = adminListenPort;
   }

   public String getAdminUserName()
   {
      return adminUserName;
   }

    /**
     * @param adminUserName The name of the Administrator user.
     */
   public void setAdminUserName(String adminUserName)
   {
      this.adminUserName = adminUserName;
   }

   public String getAdminPassword()
   {
      return adminPassword;
   }

    /**
     * @param adminPassword The password of the Administrator user.
     */
   public void setAdminPassword(String adminPassword)
   {
      this.adminPassword = adminPassword;
   }

   public String getWlsHome()
   {
      return wlsHome;
   }

    /**
     * @deprecated Use the wlHome property.
     * 
     * @param wlsHome The location of the local WebLogic Server installation. The parent directory of this location is usually
     *        named wlserver_x.y. The directory must also contain the 'common' and 'server' subdirectories.
     */
   public void setWlsHome(String wlsHome)
   {
      this.wlsHome = wlsHome;
   }
   
   public String getWlHome()
   {
      return wlHome;
   }

    /**
     * 
     * @param wlHome The location of the local WebLogic Server installation. The parent directory of this location is usually
     *        named wlserver_x.y. The directory must also contain the 'common' and 'server' subdirectories. Defaults to the
     *        value of the WL_HOME environment variable.
     */
   public void setWlHome(String wlHome)
   {
      this.wlHome = wlHome;
   }

   public String getTarget()
   {
      return target;
   }

    /**
     * @param target The name of the target for the deployment. This can be the name of the Admin Server i.e. "AdminServer", the
     *        name of an individual Managed Server or the name of a Cluster (not yet supported).
     */
   public void setTarget(String target)
   {
      this.target = target;
   }

   public String getWeblogicJarPath()
   {
      return weblogicJarPath;
   }

    /**
     * @param weblogicJarPath The location of weblogic.jar (optional)
     */
   public void setWeblogicJarPath(String weblogicJarPath)
   {
      this.weblogicJarPath = weblogicJarPath;
   }
   
   public String getJmxClientJarPath()
   {
      return jmxClientJarPath;
   }

    /**
     * @param jmxClientJarPath The location of wljmxclient.jar or an equivalent library. (optional)
     */
   public void setJmxClientJarPath(String jmxClientJarPath)
   {
      this.jmxClientJarPath = jmxClientJarPath;
   }

   public String getJmxProtocol()
   {
      return jmxProtocol;
   }

    /**
     * @param jmxProtocol The protocol to use, when connecting to the WebLogic Domain Runtime MBean Server. (optional)
     */
   public void setJmxProtocol(String jmxProtocol)
   {
      this.jmxProtocol = jmxProtocol;
   }

   public String getJmxHost()
   {
      return jmxHost;
   }

    /**
     * @param jmxHost The host where the WebLogic Domain Runtime MBean Server resides. (optional)
     */
   public void setJmxHost(String jmxHost)
   {
      this.jmxHost = jmxHost;
   }

   public int getJmxPort()
   {
      return jmxPort;
   }

    /**
     * @param jmxPort The port that the WebLogic Domain Runtime MBean Server listens on. (optional)
     */
   public void setJmxPort(int jmxPort)
   {
      this.jmxPort = jmxPort;
   }

   public boolean isUseDemoTrust()
   {
      return useDemoTrust;
   }

    /**
     * @param useDemoTrust Use the Demo Truststore, to connect to a WebLogic Server that uses Demo Identity and Trust stores.
     */
   public void setUseDemoTrust(boolean useDemoTrust)
   {
      this.useDemoTrust = useDemoTrust;
   }

   public boolean isUseCustomTrust()
   {
      return useCustomTrust;
   }

    /**
     * @param useCustomTrust Use a custom Truststore, to connect to a WebLogic Server that uses a Custom Trust store.
     */
   public void setUseCustomTrust(boolean useCustomTrust)
   {
      this.useCustomTrust = useCustomTrust;
   }

   public boolean isUseJavaStandardTrust()
   {
      return useJavaStandardTrust;
   }

    /**
     * @param useJavaStandardTrust Use a the Truststore of the running JRE, to connect to a WebLogic Server that uses the Java
     *        Standard Trust store.
     */
   public void setUseJavaStandardTrust(boolean useJavaStandardTrust)
   {
      this.useJavaStandardTrust = useJavaStandardTrust;
   }

   public String getTrustStoreLocation()
   {
      return trustStoreLocation;
   }

    /**
     * @param trustStoreLocation The location of the truststore. This should be specified when using a custom trust store. This
     *        is computed internally, when using the Java Standard Trust or the Demo Trust store.
     */
   public void setTrustStoreLocation(String trustStoreLocation)
   {
      this.trustStoreLocation = trustStoreLocation;
   }

   public String getTrustStorePassword()
   {
      return trustStorePassword;
   }

   /**
    * @param trustStorePassword The password for the trust store.
    */
   public void setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword = trustStorePassword;
   }

   public boolean isIgnoreHostNameVerification()
   {
      return ignoreHostNameVerification;
   }

    /**
     * @param ignoreHostNameVerification Specifies whether hostname verification should be enabled or disabled for the
     *        weblogic.Deployer process.
     */
   public void setIgnoreHostNameVerification(boolean ignoreHostNameVerification)
   {
      this.ignoreHostNameVerification = ignoreHostNameVerification;
   }

   public String getHostnameVerifierClass()
   {
      return hostnameVerifierClass;
   }

    /**
     * @param hostnameVerifierClass The fully qualified class name of the hostname verifier class
     */
   public void setHostnameVerifierClass(String hostnameVerifierClass)
   {
      this.hostnameVerifierClass = hostnameVerifierClass;
   }

   public String getClassPath()
   {
      return classPath;
   }

    /**
     * @param classPath The classpath entries that will be added to the classpath used by weblogic.Deployer. The location of the
     *        hostname verifier class can be provided via this property.
     */
   public void setClassPath(String classPath)
   {
      this.classPath = classPath;
   }
   
   public boolean isUseURandom()
   {
      return useURandom;
   }

   /**
    * 
    * @param useURandom Enables the use of /dev/urandom as the entropy gathering device for the JVM used to launch
    *        weblogic.Deployer. To be used in Linux/Unix.
    */
   public void setUseURandom(boolean useURandom)
   {
      this.useURandom = useURandom;
   }

}
