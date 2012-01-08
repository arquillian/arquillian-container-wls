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
package org.jboss.arquillian.container.wls.jsr88_12c.clientutils;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.shrinkwrap.api.Archive;

public interface WeblogicClient {
	
    /**
	 * Start-up the WebLogic JSR-88 deployment tool
	 * 
	 *  -   Get the node addresses list associated with the target
	 *  -   Check the status of the target server instance
	 *  -   In case of cluster tries to fund an instance which has
	 *		RUNNING status 
	 * 
	 * @param none
	 * @return none
	 */    
    public void startUp();
    	
    /**
	 * Do deploy an application or a module contained by the archive
	 * to the target (target can be a server, a cluster of servers, or 
	 * a virtual server)
	 * 
	 * @param name		- name temporary file containing the deployment 
	 * 		  archive	- archive to be deployed 
	 * @return HTTPContext - the HTTPContext for requests
	 */
    public HTTPContext doDeploy(Archive<?> archive) throws DeploymentException; 
	
    /**
	 * Do undeploy the application 
	 * 
	 * @param name 		- application name
	 * 		  archive	- archive to be deployed 
	 * @return none
	 */
	public void doUndeploy(Archive<?> archive) throws DeploymentException;
	
    /**
	 * Do undeploy the application 
	 * 
	 * @param name 			- application name
	 * @return responseMap
	 */
	public void shutDown();	
    
}
