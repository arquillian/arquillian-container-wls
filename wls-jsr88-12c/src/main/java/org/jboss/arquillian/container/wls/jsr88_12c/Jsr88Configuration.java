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
import org.jboss.arquillian.container.spi.client.deployment.Validate;

public class Jsr88Configuration {

	/*
	 * Default values for GlassFish
	 */
	public final static String 	DEFAULT_HOST 	= "localhost";
	public final static String	DEFAULT_PORT 	= "4848";
	public final static String 	DEFAULT_TARGET 	= "server";
	public final static String 	DMANAGER_URI 	= "deployer:Sun:AppServer::";
	public final static String 	DFACTORY_CLASS 	= "org.glassfish.deployapi.SunDeploymentFactory";

	public Jsr88Configuration() {
		setAdminHost(DEFAULT_HOST);
		setAdminPort(DEFAULT_PORT);
		setTarget(DEFAULT_TARGET);
	}

	/**
	 * Admin Server host address.
	 * Used to build the URI for requests.
	 */
	private String adminHost;
	
	/**
	 * Admin Console HTTP port.
	 * Used to build the URI for requests.
	 */
	private String adminPort;
	
    /**
     * Authorised admin user in the remote realm
     */
    private String adminUser;
	
    /**
     * Authorised admin user password
     */
    private String adminPassword;
	
	/**
	 * Specifies the target to which you are  deploying. 
	 * 
	 * Valid values are:
	 * 	AdminServer
	 *   	Deploys the component to the default Admin Server instance.
	 *   	This is the default value.
	 *   server_name
	 *   	Deploys the component to  a  particular  stand-alone sever.
	 *   cluster_name
	 *   	Deploys the component to every  server  instance  in
	 *   	the cluster. (Though Arquillion use only one instance
	 *   	to run the test case.)
	 *   virtul_host (not implemented yet)
	 *   	Deploys the component to the virtual-host, that can 
	 *      represent a server or a cluster of servers.
	 */
    private String target;
	
	public String getAdminHost()
	{
		return adminHost;
	}	
	
	public void setAdminHost(String adminHost)
	{
		this.adminHost = adminHost;
	}
	
	public String getAdminPort()
	{
		return adminPort;
	}
	
	
	public void setAdminPort(String adminPort)
	{
		this.adminPort = adminPort;
	}
	
	
    public String getAdminUser() {
        return adminUser;
    }
	
    public void setAdminUser(String adminUser) {
        this.adminUser = adminUser;
    }
	
    public String getAdminPassword() {
        return adminPassword;
    }
	
    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
	
	public String getTarget() {
		return target; 
	}
	
	public void setTarget(String target) {
		this.target = target; 
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
