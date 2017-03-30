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
package org.jboss.arquillian.container.wls;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;

/**
 * A utility class that encapsulates the logic for creation of a {@link HTTPContext} instance.
 *
 * @author Vineet Reynolds
 */
class HttpContextBuilder {

    private static final String RUNNING = "RUNNING";

    /**
     * The context that is created. This will be returned to the client, once it is completely built.
     */
    private HTTPContext httpContext;

    /**
     * The deployment for which the context must be built.
     */
    private String deploymentName;

    private CommonWebLogicConfiguration configuration;
    private MBeanServerConnection connection;
    private ObjectName domainRuntimeService;

    /**
     * The set of Server Runtime MBeans to use for preparing the context. This will be one in the case of a deployment
     * against a
     * single managed server, and multiple for a deployment against a cluster.
     */
    private ObjectName[] wlServerRuntimes;

    public HttpContextBuilder(String deploymentName, CommonWebLogicConfiguration configuration,
        MBeanServerConnection connection, ObjectName domainRuntimeService) {
        this.deploymentName = deploymentName;
        this.configuration = configuration;
        this.connection = connection;
        this.domainRuntimeService = domainRuntimeService;
    }

    public HTTPContext createContext() throws Exception {
        // First, get the deployment in the domain configuration
        // that matches the deployment made by Arquillian.
        ObjectName appDeployment = findMatchingDeployment(deploymentName);
        if (appDeployment == null) {
            throw new DeploymentException("The specified deployment could not be found in the MBean Server.\n"
                + "The deployment must have failed. Verify the output of the weblogic.Deployer process.");
        }
        // Get the targets for the deployment. For now, there will be a single target
        // This will be either a managed server or a cluster.
        ObjectName[] targets = (ObjectName[]) connection.getAttribute(appDeployment, "Targets");
        for (ObjectName target : targets) {
            String targetType = (String) connection.getAttribute(target, "Type");
            String targetName = (String) connection.getAttribute(target, "Name");
            if (targetName.equals(configuration.getTarget())) {
                if (targetType.equals("Server")) {
                    // Get the Server Runtime MBean, that will be used to create the context.
                    wlServerRuntimes = findRunningWLServerRuntimes(targetName);
                    buildHTTPContext();
                } else if (targetType.equals("Cluster")) {
                    // Get all the Server Runtime MBeans for the servers in the cluster,
                    // that will be used to create the context.
                    String[] clusterMemberNames = findMembersOfCluster(target);
                    wlServerRuntimes = findRunningWLServerRuntimes(clusterMemberNames);
                    buildHTTPContext();
                }
                break;
            }
        }
        if (httpContext == null) {
            throw new DeploymentException(
                "An unexpected condition was encountered. The HTTPContext could not be created.");
        } else {
            return httpContext;
        }
    }

    /**
     * Creates the {@link HTTPContext} instance, with the required preconditions in place.
     *
     * @throws Exception
     *     When an exception is encountered during creation of the context.
     */
    private void buildHTTPContext() throws Exception {
        // If there are no running servers, we'll abort as the test cannot be executed.
        if (wlServerRuntimes.length < 1) {
            throw new DeploymentException("None of the targets are in the RUNNING state.");
        } else {
            // For now, we'll use the first server to populate the context.
            // This may change in a future Arquillian release,
            // to allow different strategies for testing a clustered deployment.
            ObjectName wlServerRuntime = wlServerRuntimes[0];
            String httpUrlAsString = (String) connection.invoke(wlServerRuntime, "getURL",
                new Object[] {"http"},
                new String[] {"java.lang.String"}
            );

            URL serverHttpUrl = new URL(httpUrlAsString);
            httpContext = new HTTPContext(serverHttpUrl.getHost(), serverHttpUrl.getPort());
            List<ObjectName> servletRuntimes = findServletRuntimes(wlServerRuntime, deploymentName);
            for (ObjectName servletRuntime : servletRuntimes) {
                String servletName = (String) connection.getAttribute(servletRuntime, "ServletName");
                String servletContextRoot = (String) connection.getAttribute(servletRuntime, "ContextPath");
                httpContext.add(new Servlet(servletName, servletContextRoot));
            }
        }
    }

    /**
     * Retrieves the names of cluster members, so that their Runtime MBeans can be fetched from the Domain Runtime MBean
     * Service.
     *
     * @param cluster
     *     The cluster whose member names are to be fetched
     *
     * @return An array of server names whose membership is in the cluster
     *
     * @throws Exception
     *     When a failure is encountered when browsing the Domain Configuration MBean Server hierarchy.
     */
    private String[] findMembersOfCluster(ObjectName cluster) throws Exception {
        ObjectName[] servers = (ObjectName[]) connection.getAttribute(cluster, "Servers");
        List<String> clusterServers = new ArrayList<String>();
        for (ObjectName server : servers) {
            String serverName = (String) connection.getAttribute(server, "Name");
            clusterServers.add(serverName);
        }

        return clusterServers.toArray(new String[0]);
    }

    /**
     * Returns a set of Runtime MBean instances for the provided WebLogic Server names. This is eventually used to create
     * the
     * HTTPContext instance with the runtime listen address and port, as only running WebLogic Server instances are
     * considered
     * for creation of the HTTPContext.
     *
     * @param runtimeNames
     *     The array of WebLogic Server instances for which the Runtime MBeans must be returned
     *
     * @return An array of {@link ObjectName} instances representing running WebLogic Server instances
     *
     * @throws Exception
     *     When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
     */
    private ObjectName[] findRunningWLServerRuntimes(String... runtimeNames) throws Exception {
        List<String> runtimeNamesList = Arrays.asList(runtimeNames);
        List<ObjectName> wlServerRuntimeList = new ArrayList<ObjectName>();
        ObjectName[] wlServerRuntimes = (ObjectName[]) connection.getAttribute(domainRuntimeService, "ServerRuntimes");

        for (ObjectName wlServerRuntime : wlServerRuntimes) {
            String runtimeName = (String) connection.getAttribute(wlServerRuntime, "Name");
            String runtimeState = (String) connection.getAttribute(wlServerRuntime, "State");
            if (runtimeNamesList.contains(runtimeName) && runtimeState.equals(RUNNING)) {
                wlServerRuntimeList.add(wlServerRuntime);
            }
        }
        return wlServerRuntimeList.toArray(new ObjectName[0]);
    }

    /**
     * Retrieves a list of Servlet Runtime MBeans for a deployment against a WebLogic Server instance. This is eventually
     * used
     * to populate the HTTPContext instance with all servlets in the deployment.
     *
     * @param wlServerRuntime
     *     The WebLogic Server runtime instance which houses the deployment
     * @param deploymentName
     *     The deployment for which the Servlet Runtime MBeans must be retrieved
     *
     * @return A list of {@link ObjectName} representing Servlet Runtime MBeans for the deployment
     *
     * @throws Exception
     *     When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
     */
    private List<ObjectName> findServletRuntimes(ObjectName wlServerRuntime, String deploymentName) throws Exception {
        ObjectName[] applicationRuntimes = (ObjectName[]) connection.getAttribute(wlServerRuntime, "ApplicationRuntimes");
        for (ObjectName applicationRuntime : applicationRuntimes) {
            String applicationName = (String) connection.getAttribute(applicationRuntime, "Name");
            if (applicationName.equals(deploymentName)) {
                ObjectName[] componentRuntimes =
                    (ObjectName[]) connection.getAttribute(applicationRuntime, "ComponentRuntimes");
                List<ObjectName> servletRuntimes = new ArrayList<ObjectName>();
                for (ObjectName componentRuntime : componentRuntimes) {
                    String componentType = (String) connection.getAttribute(componentRuntime, "Type");
                    if (componentType.toString().equals("WebAppComponentRuntime")) {
                        servletRuntimes.addAll(
                            Arrays.asList((ObjectName[]) connection.getAttribute(componentRuntime, "Servlets")));
                    }
                }
                return servletRuntimes;
            }
        }

        throw new DeploymentException(
            "The deployment details were not found in the MBean Server. Possible causes include:\n"
                + "1. The deployment failed. Review the admin server and the target's log files.\n"
                + "2. The deployment succeeded partially. The deployment must be the Active state. Instead, it might be in the 'New' state.\n"
                + "   Verify that the the admin server can connect to the target(s), and that no firewall rules are blocking the traffic on the admin channel.");
    }

    /**
     * Retrieves an Application Deployment MBean for a specified deployment. This may return <code>null</code> if the
     * specified
     * deployment is not found, so that this method may be used by both the deployment and undeployment routines to verify
     * if a
     * deployment is available, or not.
     *
     * @param deploymentName
     *     The deployment whose MBean must be retrieved
     *
     * @return An {@link ObjectName} representing the Application Deployment MBean for the deployment. This returns
     * <code>null</code> if a deployment is not found.
     *
     * @throws Exception
     *     When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
     */
    public ObjectName findMatchingDeployment(String deploymentName) throws Exception {
        ObjectName[] appDeployments = findAllAppDeployments();
        for (ObjectName appDeployment : appDeployments) {
            String appDeploymentName = (String) connection.getAttribute(appDeployment, "Name");
            if (appDeploymentName.equals(deploymentName)) {
                return appDeployment;
            }
        }

        return null;
    }

    /**
     * Obtains all the deployments in a WebLogic domain
     *
     * @return An array of {@link ObjectName} corresponding to all deployments in a WebLogic domain.
     *
     * @throws Exception
     *     When a failure is encountered when browsing the Domain Runtime MBean Server hierarchy.
     */
    private ObjectName[] findAllAppDeployments() throws Exception {
        ObjectName domainConfig = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainConfiguration");
        return (ObjectName[]) connection.getAttribute(domainConfig, "AppDeployments");
    }
}
