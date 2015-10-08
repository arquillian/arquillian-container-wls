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
package org.jboss.arquillian.container.wls.remote.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.wls.WebLogicRemoteContainer;
import org.jboss.arquillian.container.wls.rest.RESTUtils;
import org.jboss.shrinkwrap.api.Archive;

import javax.ws.rs.client.Client;

/**
 * A utility class for performing operations relevant to a remote WebLogic
 * container used by Arquillian. Relies completely on the REST client to perform
 * deployments. WLS 12.1.3 containers and higher are encouraged to use this
 * class.
 * 
 * @author Vineet Reynolds
 *
 */
public class RemoteContainer implements WebLogicRemoteContainer {
	private static final Logger LOGGER = Logger.getLogger(RemoteContainer.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}

	protected WebLogicRemoteConfiguration config;


	public RemoteContainer(WebLogicRemoteConfiguration configuration) {
		config = configuration;
	}

	/**
	 * Deploy an application.
	 *
	 * @param archive The ShrinkWrap archive to deploy
   *
	 * @return The metadata for the deployed application
   *
	 * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
	 */
	@SuppressWarnings("resource")
	public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
    return RESTUtils.deploy(config, LOGGER, archive);
	}


	/**
	 * Undeploy an application.
	 *
	 * @param archive The ShrinkWrap archive to undeploy
   *
	 * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
	 */
	public void undeploy(Archive<?> archive) throws DeploymentException {
    RESTUtils.undeploy(config, LOGGER, archive);
	}

}
