package org.jboss.arquillian.container.wls.remote_103x;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class WebLogicExtension implements LoadableExtension
{

   public void register(ExtensionBuilder builder)
   {
      builder.service(DeployableContainer.class, WebLogicContainer.class)
             .service(ProtocolArchiveProcessor.class, WebLogicCDIProcessor.class);
   }

}
