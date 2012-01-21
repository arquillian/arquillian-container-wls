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
	
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;

import weblogic.server.ServerStates;


public class WeblogicJmxClient {

   private MBeanServerConnection connection;
   private JMXConnector connector = null;
   private ObjectName service;
   private final String WEB_APPLICATION = "WebAppComponentRuntime"; 

   // Initializing the object name for DomainRuntimeServiceMBean
   public WeblogicJmxClient() {
	  
      try {
          service = new ObjectName("com.bea:Name=DomainRuntimeService" 
          + ",Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
       }catch (MalformedObjectNameException e) {
          throw new WeblogicClientException(e.getMessage());
       }
   }

   /*
   * Initialize connection to the Domain Runtime MBean Server
   */
   public void initConnection(String hostname, String portString, String username, String password) {
	   
	   if (connector == null) {
			String protocol = "t3";
			Integer portInteger = Integer.valueOf(portString);
			int port = portInteger.intValue();
			String jndiroot = "/jndi/";
			String mserver = "weblogic.management.mbeanservers.domainruntime";
			
			JMXServiceURL serviceURL = null;
			try {
				serviceURL = new JMXServiceURL(protocol, hostname, port, jndiroot + mserver);
			} catch (MalformedURLException e) {
				throw new WeblogicClientException(e.getMessage());
			}
				Hashtable<String, String> parameters = new Hashtable<String, String>();
				parameters.put(Context.SECURITY_PRINCIPAL, username);
				parameters.put(Context.SECURITY_CREDENTIALS, password);
				parameters.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
			try {
				connector = JMXConnectorFactory.connect(serviceURL, parameters);
				connection = connector.getMBeanServerConnection();
			} catch (IOException e) {
				throw new WeblogicClientException(e.getMessage());
			}
	   }
   }

   /*
   * Close the JSR77 connection
   */
   public void closeConnection() throws Exception {
	   if (connector != null) {
		   connector.close();
		   connector = null;
	   } 
   }
   
   
   /*
   * Get an array of WebAppComponentRuntimeMBeans
   */
   public HTTPContext getHTTPContext(String applicationName, List<String> serverNameList) throws Exception {
	  
	   HTTPContext httpContext = null;
	   boolean applicationMathced = false;	  

	  List<ObjectName> serverRuntimeList = getServerRuntimes(serverNameList);
	  for (ObjectName serverRuntime : serverRuntimeList) {

    	 // String listenAddress = (String) connection.getAttribute(serverRuntime, "ListenAddress");
    	 // String listenPort = (String) connection.getAttribute(serverRuntime, "ListenPort");
		 String defaultURL = (String) connection.getAttribute(serverRuntime, "DefaultURL");
		 URI contextURI = URI.create( defaultURL );		    	  
		 httpContext = new HTTPContext( contextURI.getHost(), contextURI.getPort() );

		 ObjectName[] appRuntimes = (ObjectName[]) connection.getAttribute(serverRuntime, "ApplicationRuntimes");

         for (ObjectName appRuntime : appRuntimes) {
        	String name = (String)connection.getAttribute(appRuntime, "Name");

        	if (applicationName.equals(name)) {

            	applicationMathced = true;
	            ObjectName[] componentRuntimes = (ObjectName[]) connection.getAttribute(appRuntime, "ComponentRuntimes");
	            for (ObjectName componentRuntime : componentRuntimes) {
	               String componentType = (String) connection.getAttribute(componentRuntime, "Type");
	               if ( WEB_APPLICATION.equals(componentType.toString()) ){
	                  ObjectName[] servletRuntimes = (ObjectName[]) connection.getAttribute(componentRuntime, "Servlets");
	                  for ( ObjectName servletRuntime : servletRuntimes ) {
	                	  String servletName = (String)connection.getAttribute(servletRuntime, "Name");
	                	  String servletContext = (String)connection.getAttribute(servletRuntime, "ContextPath");
	                	  httpContext.add( new Servlet(servletName, servletContext) );	                	  
	                  }
	               } 
	            } 
            } // end if - application name is equal

          }
     	  if ( applicationMathced ) {
     		  // We have found the deployed application, finish the lookup
     		  break;
     	  }
         
      } // end for - serverRuntimes
	  return httpContext;
   }

   
   /*
   * Get an list of ServerRuntimeMBeans filtered by has 
   * a status of RUNNING and member of the serverNameList
   */
   private List<ObjectName> getServerRuntimes(List<String> serverNameList) { 

	   List<ObjectName> serverRuntimeList = new ArrayList<ObjectName>();
	   ObjectName[] serverRuntims = null;
		try {
			serverRuntims = (ObjectName[]) connection.getAttribute(service, "ServerRuntimes");
			String serverName;
			for (ObjectName serverRuntime : serverRuntims) {
				serverName = (String) connection.getAttribute(serverRuntime, "Name");
				if (serverNameList.contains(serverName)) {
					if ( ServerStates.RUNNING.equals((String) connection.getAttribute(serverRuntime, "State")) ) {
						serverRuntimeList.add(serverRuntime);
					}
				}
			}
		} catch (Exception e) {
			throw new WeblogicClientException(e.getMessage());
		}
		if ( serverRuntimeList.size() < 1) {
			throw new WeblogicClientException("There is no server with RUNNING status on your deployment taget.");
		}
		return serverRuntimeList;
   }

}


