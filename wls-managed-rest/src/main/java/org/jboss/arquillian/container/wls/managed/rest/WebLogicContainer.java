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
package org.jboss.arquillian.container.wls.managed.rest;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
//import org.jboss.arquillian.container.wls.jmx.ManagedContainer;
import org.jboss.arquillian.container.wls.managed.rest.ManagedContainer;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;


/**
 * Managed WebLogic 12.1.x container
 * 
 * @author Vineet Reynolds
 * 
 */
public class WebLogicContainer implements DeployableContainer<WebLogicManagedConfiguration> {

    private WebLogicManagedConfiguration configuration;
    private ManagedContainer managedContainer;

    @Override
    public Class<WebLogicManagedConfiguration> getConfigurationClass() {
        return WebLogicManagedConfiguration.class;
    }

    @Override
    public void setup(WebLogicManagedConfiguration configuration) {
        this.configuration = configuration;
        this.managedContainer = new ManagedContainer(this.configuration);
    }

    @Override
    public void start() throws LifecycleException {
        managedContainer.start();
    }

    @Override
    public void stop() throws LifecycleException {
        managedContainer.stop();
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return managedContainer.deploy(archive);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        managedContainer.undeploy(archive);
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

}
