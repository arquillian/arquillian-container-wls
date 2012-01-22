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
package org.jboss.arquillian.container.wls.remote_12_1;

import java.io.File;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.wls.ShrinkWrapUtil;
import org.jboss.arquillian.container.wls.WebLogicDeployerClient;
import org.jboss.arquillian.container.wls.WebLogicJMXClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

/**
 * WebLogic 12.1.x container
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicContainer implements DeployableContainer<WebLogicRemoteConfiguration>
{
   
   WebLogicRemoteConfiguration configuration;
   private WebLogicDeployerClient deployerClient;
   private WebLogicJMXClient jmxClient;

   public Class<WebLogicRemoteConfiguration> getConfigurationClass()
   {
      return WebLogicRemoteConfiguration.class;
   }

   public void setup(WebLogicRemoteConfiguration configuration)
   {
      this.configuration = configuration;
   }

   public void start() throws LifecycleException
   {
      deployerClient = new WebLogicDeployerClient(configuration);
      jmxClient = new WebLogicJMXClient(configuration);
   }

   public void stop() throws LifecycleException
   {
      jmxClient.close();
   }

   public ProtocolDescription getDefaultProtocol()
   {
      return new ProtocolDescription("Servlet 3.0");
   }

   public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException
   {
      String deploymentName = getDeploymentName(archive);
      File deploymentArchive = ShrinkWrapUtil.toFile(archive);
      
      deployerClient.deploy(deploymentName, deploymentArchive);
      ProtocolMetaData metadata = jmxClient.deploy(deploymentName);
      return metadata;
   }

   public void undeploy(Archive<?> archive) throws DeploymentException
   {
      // Undeploy the application
      String deploymentName = getDeploymentName(archive);
      deployerClient.undeploy(deploymentName);
      
      // Verify the undeployment from the Domain Runtime MBean Server.
      jmxClient.undeploy(deploymentName);
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
      if(indexOfDot != -1)
      {
         return archiveFilename.substring(0, indexOfDot);
      }
      return archiveFilename;
   }

}
