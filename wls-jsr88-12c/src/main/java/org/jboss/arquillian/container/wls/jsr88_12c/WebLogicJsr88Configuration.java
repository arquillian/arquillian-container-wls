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

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;
import org.jboss.arquillian.container.spi.client.deployment.Validate;

public class WebLogicJsr88Configuration extends Jsr88Configuration implements ContainerConfiguration {

	/*
	 * Default values for WebLogic
	 */
	public final static String 	DEFAULT_HOST 	= "localhost";
	public final static String 	DEFAULT_PORT 	= "7001";
	public final static String 	DEFAULT_TARGET 	= "AdminServer";
	public final static String 	DMANAGER_URI 	= "deployer:WebLogic:t3://";
	public final static String 	DFACTORY_CLASS 	= "weblogic.deploy.api.spi.factories.internal.DeploymentFactoryImpl";
	
	public WebLogicJsr88Configuration() {
		setAdminHost(DEFAULT_HOST);
		setAdminPort(DEFAULT_PORT);
		setTarget(DEFAULT_TARGET);
	}	
	
    /**
     * Validates if current configuration is valid, that is if all required
     * properties are set and have correct values
     */
    public void validate() throws ConfigurationException {
		Validate.notNull(getAdminUser(), "adminUser property must be specified in your arquillian.xml");
		Validate.notNull(getAdminPassword(), "adminPassword property must be specified in your arquillian.xml");
    }

}
