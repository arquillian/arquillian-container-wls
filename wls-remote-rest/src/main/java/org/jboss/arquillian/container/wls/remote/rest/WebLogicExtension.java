package org.jboss.arquillian.container.wls.remote.rest;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * The Arquillian integration for a remote WebLogic Server container.
 *
 * @author Vineet Reynolds
 */
public class WebLogicExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(DeployableContainer.class, WebLogicContainer.class);
    }
}
