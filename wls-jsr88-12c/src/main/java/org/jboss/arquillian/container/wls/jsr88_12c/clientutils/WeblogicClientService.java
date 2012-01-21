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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.wls.jsr88_12c.WebLogicJsr88Configuration;
import org.jboss.shrinkwrap.api.Archive;

import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;

import weblogic.deploy.api.spi.WebLogicDeploymentManager;
import weblogic.deploy.api.spi.WebLogicTarget;
import weblogic.deploy.api.spi.factories.WebLogicDeploymentFactory;
import weblogic.deploy.api.tools.SessionHelper;
import weblogic.management.configuration.ServerMBean;

public class WeblogicClientService extends Jsr88ClientService implements WeblogicClient {

	WebLogicDeploymentManager deploymentManager;
	private WeblogicJmxClient jmxClient;

	private static final Logger log = Logger.getLogger(WeblogicClientService.class.getName());
	
	public WeblogicClientService( WebLogicJsr88Configuration configuration ) {
		super(configuration);
	}

	@Override
	public void startUp() throws WeblogicClientException {

		// Setup the connection of WebLogic JSR-88 Deployment implementation
		final String deploymentManagerURI = WebLogicJsr88Configuration.DMANAGER_URI +
				configuration.getAdminHost() + ":" + configuration.getAdminPort();
		try {
			connectDeploymentManager( WebLogicJsr88Configuration.DFACTORY_CLASS,
					deploymentManagerURI, 
					configuration.getAdminHost(),
					configuration.getAdminPort(),
					configuration.getAdminUser(),
					configuration.getAdminPassword() );
		} catch (Exception ch) {
	        throw new WeblogicClientException( "Could not connect to the admin server on: " 
	        		+ deploymentManagerURI + " | " + ch.getMessage() );
	    }

		// Setup the connection of WebLogic JMX implementation 
		jmxClient = new WeblogicJmxClient();
		jmxClient.initConnection(configuration.getAdminHost(), 
				configuration.getAdminPort(),
				configuration.getAdminUser(),
				configuration.getAdminPassword() );
	}


	@Override
	public HTTPContext doDeploy(Archive<?> archive) throws WeblogicClientException {		

		// Delegate effective work to the (standard) JSR-88 Deployment layer 
		HTTPContext httpContext = super.doDeploy(archive);

		/* 
		 * We have deployed the archive successfully. Next we have to construct
		 * the HttpContext for the Aquillian container ProtocolMetaData
		 */
		String deploymentName = createDeploymentName(archive.getName());
		// Get the list of server's names targeted by deployment
		List<String> serverNameList = getDeploymentServerList( configuration.getTarget() );
		try {
			httpContext = jmxClient.getHTTPContext(deploymentName, serverNameList);
		} catch (Exception e) {
			throw new WeblogicClientException( "Could not construct the httpContext: " + e.getMessage() );
		}		
		return httpContext;
	}

	
	@Override
	public void shutDown() {

		// Release the JSR-88 Deployment connection
		super.shutDown();

		// Release the JMX connection
		try {
			jmxClient.closeConnection();
		} catch (Exception e) {
			throw new WeblogicClientException( "Failed to close the JMX connection: " + e.getMessage() );
		}

	}


	@Override
	protected WebLogicDeploymentManager connectDeploymentManager(String factoryClass, String uri,
		   String host, String port, String username, String password) throws Exception {

	  if (getDeploymentManager() == null) {
		 final String deploymentManagerURI = WebLogicJsr88Configuration.DMANAGER_URI +
					configuration.getAdminHost() + ":" + configuration.getAdminPort();
         DeploymentFactoryManager dfm = DeploymentFactoryManager.getInstance();
         WebLogicDeploymentFactory df = (WebLogicDeploymentFactory) Class.forName(factoryClass).newInstance();
         if ( df.handlesURI(deploymentManagerURI) ) {
             dfm.registerDeploymentFactory( (DeploymentFactory) Class.forName(factoryClass).newInstance());
        	 
             // Get the WebLogic JSR-88 Deployment Manager
             setDeploymentManager( SessionHelper.getDeploymentManager(host, port, username, password) );
             /* Alternative way to get a WebLogic JSR-88 Deployment Manager
             setDeploymentManager( (WebLogicDeploymentManager)df.getDeploymentManager(
        			 df.createUri(WebLogicDeploymentFactory.REMOTE_DM_URI, host, port),
        			username, password) );
              */

             log.info( "DeploymentManager has connected successfully to the server on adminHost=" 
        			 + host + " adminPort=" + port);        	 

         } else {
			 throw new WeblogicClientException( "The provided URI, host & port " + deploymentManagerURI 
					 + " is not supported." );
         }
      }

	  return (WebLogicDeploymentManager) getDeploymentManager();
   }

	
   /**
   * Get a list of server names that has been associated with the target 
   * parameter of deployment
   *
   * @param target - the target specified by deployment configuration can be
   * 				 a server, a cluster of servers or a VirtualServer 
   * @return list names of the server(s) targeted by deployment
   * 
   */
	private List<String> getDeploymentServerList(String deploymentTartget) {

		List<String> serverNameList = new ArrayList<String>();
		WebLogicTarget wtarget = (WebLogicTarget) 
				((WebLogicDeploymentManager) getDeploymentManager()).getTarget(deploymentTartget);

		if (wtarget.isServer() ) {
			serverNameList.add(wtarget.getName());
		} else if ( wtarget.isCluster() ) {
			ServerMBean[] serverMBeans = deploymentManager.getHelper().getDomain()
					.lookupCluster(configuration.getTarget()).getServers();
			for (ServerMBean serverMBean : serverMBeans ) {
				serverNameList.add(serverMBean.getName());
			}
		} else if ( wtarget.isVirtualHost() ) {
			throw new WeblogicClientException( "Deployment to VirtualHost not yet implemented." );			
		}
		return serverNameList;
	}

}
