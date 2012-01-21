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

/**
 *
 * @author Z.Paulovics
 */
package org.jboss.arquillian.container.wls.jsr88_12c;

import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.wls.jsr88_12c.clientutils.WeblogicClient;
import org.jboss.arquillian.container.wls.jsr88_12c.clientutils.WeblogicClientException;
import org.jboss.arquillian.container.wls.jsr88_12c.clientutils.WeblogicClientService;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

public class WeblogicJsr88DeployableContainer implements DeployableContainer<WebLogicJsr88Configuration> {

	private WebLogicJsr88Configuration configuration;
	private WeblogicClient weblogicClient;
	
	private static final Logger log = Logger.getLogger(WeblogicJsr88DeployableContainer.class.getName());

	public WeblogicJsr88DeployableContainer() {	}

	public Class<WebLogicJsr88Configuration> getConfigurationClass() {
		return WebLogicJsr88Configuration.class;
	}

	public void setup(WebLogicJsr88Configuration configuration) {

		if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }
        setConfiguration(configuration);
        
        // Start up the weblogicClient service layer
        this.weblogicClient = new WeblogicClientService(configuration);
	}

	public void start() throws LifecycleException {

    	try {			
    		weblogicClient.startUp();    		
		} catch (WeblogicClientException e) {
    		log.severe( e.getMessage() );
    		throw new LifecycleException( e.getMessage() );
        }		
	}

	public void stop() throws LifecycleException {
		weblogicClient.shutDown();    		
	}

	public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
	}

	public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {

		if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        ProtocolMetaData protocolMetaData = new ProtocolMetaData();

        try {
        	HTTPContext httpContext = weblogicClient.doDeploy(archive);
            protocolMetaData.addContext(httpContext);
            
        } catch (WeblogicClientException e) {
            throw new DeploymentException("Could not deploy " + archive.getName() + "; " + e.getMessage(), e);
        }
        
		// log.info( protocolMetaData.toString() );
        return protocolMetaData;
	}

	public void undeploy(Archive<?> archive) throws DeploymentException {

		if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        try {
        	weblogicClient.doUndeploy(archive);
        } catch (WeblogicClientException e) {
            throw new DeploymentException("Could not undeploy " + archive.getName() + "; " + e.getMessage(), e);
        }
	}

	public void deploy(Descriptor descriptor) throws DeploymentException {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void undeploy(Descriptor descriptor) throws DeploymentException {
		throw new UnsupportedOperationException("Not implemented");
	}

	protected WebLogicJsr88Configuration getConfiguration() {
		return configuration;
	}

	protected void setConfiguration(WebLogicJsr88Configuration configuration) {
		this.configuration = configuration;
	}

}
