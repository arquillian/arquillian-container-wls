package org.jboss.arquillian.container.wls.remote_103x;

import java.io.File;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class WebLogicContainer implements DeployableContainer<WebLogicConfiguration>
{
   
   WebLogicConfiguration configuration;

   public Class<WebLogicConfiguration> getConfigurationClass()
   {
      return WebLogicConfiguration.class;
   }

   public void setup(WebLogicConfiguration configuration)
   {
      this.configuration = configuration;
   }

   public void start() throws LifecycleException
   {
      //no-op
   }

   public void stop() throws LifecycleException
   {
      //no-op
   }

   public ProtocolDescription getDefaultProtocol()
   {
      // 10.3.1-10.3.5 supports Servlet Spec 2.5 officially.
      return new ProtocolDescription("Servlet 2.5");
   }

   public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException
   {
      String deploymentName = getDeploymentName(archive);
      File deploymentArchive = ShrinkWrapUtil.toFile(archive);
      WebLogicJMXClient weblogicClient = new WebLogicJMXClient(configuration);
      ProtocolMetaData metadata = weblogicClient.deploy(deploymentName, deploymentArchive);
      return metadata;
   }

   public void undeploy(Archive<?> archive) throws DeploymentException
   {
      String deploymentName = getDeploymentName(archive);
      WebLogicJMXClient weblogicClient = new WebLogicJMXClient(configuration);
      weblogicClient.undeploy(deploymentName);
   }

   public void deploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   public void undeploy(Descriptor descriptor) throws DeploymentException
   {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   private String getDeploymentName(Archive<?> archive)
   {
      String archiveFilename = archive.getName();
      int indexOfDot = archiveFilename.indexOf(".");
      if(indexOfDot == -1)
      {
         return archiveFilename.substring(0, indexOfDot);
      }
      return archiveFilename;
   }

}
