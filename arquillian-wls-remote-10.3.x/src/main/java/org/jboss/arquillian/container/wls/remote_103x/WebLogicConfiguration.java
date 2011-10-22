package org.jboss.arquillian.container.wls.remote_103x;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class WebLogicConfiguration implements ContainerConfiguration
{

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
    * the name of an individual Managed Server or the name of a Cluster.
    * 
    * The default is "AdminServer", but this is not recommended
    * for simulating a production environment during integration testing. 
    */
   private String target = "AdminServer";
   
   public void validate() throws ConfigurationException
   {
      //TODO: no-op for now. Validation logic to be built in.
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

}
