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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;

/**
 * A JMX client that connects to the Domain Runtime MBean Server to obtain
 * information about Arquillian deployments, as well as the state of the target
 * server for deployment.
 *
 * This JMX client relies on Oracle WebLogic's implementation of HTTP and IIOP
 * protocols, while also supporting the T3 protocol (as IIOP).
 *
 * Details in this area are covered by the Oracle Fusion Middleware Guide on
 * "Developing Custom Management Utilities With JMX for Oracle WebLogic Server".
 *
 * @author Vineet Reynolds
 *
 */
public class WebLogicJMXClient {

    private static final ThreadLocal<String> trustStorePath = new ThreadLocal<String>();
    private static final ThreadLocal<String> trustStorePassword = new ThreadLocal<String>();

    private CommonWebLogicConfiguration configuration;
    private MBeanServerConnection connection;
    private JMXConnector connector;
    private ObjectName domainRuntimeService;
    private ClassLoader jmxLibraryClassLoader;

    public WebLogicJMXClient(CommonWebLogicConfiguration configuration) throws LifecycleException {
        this.configuration = configuration;
        try {
            this.domainRuntimeService = new ObjectName(
                    "com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean"
            );
        } catch (MalformedObjectNameException objectNameEx) {
            // We're pretty much in trouble now. The constructed object will be useless.
            throw new IllegalStateException(objectNameEx);
        }

        try {
            setConfiguredTrustStore();

            // Now, create a connection to the Domain Runtime MBean Server.
            initWebLogicJMXLibClassLoader();
            createConnection();
        } finally {
            revertToInitialState();
        }
    }

    /**
     * Deploys an archive and verifies it was indeed deployed correctly.
     *
     * @param deploymentName the name of the deployment
     * @param deploymentArchive the archive that is to be deployed
     * @return A {@link ProtocolMetaData} object containing details of the
     * deployment
     * @throws DeploymentException When there is a failure obtaining details of
     * the deployment from the Domain Runtime MBean server.
     */
    public ProtocolMetaData deploy(String deploymentName, File deploymentArchive) throws DeploymentException {
        doDeploy(deploymentName, deploymentArchive);
        return verifyDeployment(deploymentName);
    }

    /**
     * Verifies and obtains details of the deployment.
     *
     * @param deploymentName The name of the deployment
     * @return A {@link ProtocolMetaData} object containing details of the
     * deployment.
     * @throws DeploymentException When there is a failure obtaining details of
     * the deployment from the Domain Runtime MBean server.
     */
    public ProtocolMetaData verifyDeployment(String deploymentName) throws DeploymentException {
        try {
            setConfiguredTrustStore();

            try {
                return new ProtocolMetaData().addContext(
                        new HttpContextBuilder(deploymentName, configuration, connection, domainRuntimeService).createContext()
                );
            } catch (Exception ex) {
                throw new DeploymentException("Failed to populate the HTTPContext with the deployment details", ex);
            }
        } finally {
            revertToInitialState();
        }
    }

    /**
     * Verifies that the application was undeployed. We do not want a subsequent
     * deployment with the same name to fail.
     *
     * @param deploymentName The name of the deployment
     * @throws DeploymentException When there is a failure obtaining details of
     * the deployment from the Domain Runtime MBean server.
     */
    public void undeploy(String deploymentName) throws DeploymentException {
        try {
            setConfiguredTrustStore();

            invokeUndeployOperation(deploymentName);

        } finally {
            revertToInitialState();
        }
    }

    /**
     * Verifies that the application has been undeployed.
     *
     * @param deploymentName The name of the application that was undeployed.
     * @throws Exception When a failure is encountered when browsing the Domain
     * Runtime MBean Server hierarchy.
     */
    public void verifyUndeployment(String deploymentName) throws DeploymentException {
        try {
            setConfiguredTrustStore();

            ObjectName deployment = null;
            try {
                deployment = new HttpContextBuilder(deploymentName, configuration, connection, domainRuntimeService)
                        .findMatchingDeployment(deploymentName);
            } catch (Exception ex) {
                throw new DeploymentException("Failed to obtain the status of the deployment.", ex);
            }

            if (deployment != null) {
                throw new DeploymentException("Failed to undeploy the deployed application.");
            }

        } finally {
            revertToInitialState();
        }
    }

    public void close() throws LifecycleException {
        try {
            setConfiguredTrustStore();

            closeConnection();
        } finally {
            revertToInitialState();
        }
    }

    private void invokeUndeployOperation(String deploymentName) throws DeploymentException {
        try {
            ObjectName domainRuntime = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainRuntime");
            ObjectName deploymentManager = (ObjectName) connection.getAttribute(domainRuntime, "DeploymentManager");

            ObjectName appDeploymentRuntime = (ObjectName) connection.invoke(deploymentManager,
                    "lookupAppDeploymentRuntime",
                    new Object[]{deploymentName}, new String[]{String.class.getName()}
            );

            ObjectName deploymentProgressObject = (ObjectName) connection.invoke(appDeploymentRuntime,
                    "undeploy",
                    new Object[]{}, new String[]{}
            );

            processDeploymentProgress(deploymentName, deploymentManager, deploymentProgressObject);
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private void doDeploy(String deploymentName, File deploymentArchive) throws DeploymentException {
        try {
            String serverName = configuration.getTarget();
            String[] targets = new String[]{serverName};
            ObjectName domainRuntime = (ObjectName) connection.getAttribute(domainRuntimeService, "DomainRuntime");
            ObjectName deploymentManager = (ObjectName) connection.getAttribute(domainRuntime, "DeploymentManager");

            ObjectName deploymentProgressObject = (ObjectName) connection.invoke(
                    deploymentManager, "deploy",
                    new Object[]{deploymentName, deploymentArchive.getAbsolutePath(), targets, null, new Properties()},
                    new String[]{String.class.getName(), String.class.getName(), String[].class.getName(), String.class.getName(), java.util.Properties.class.getName()}
            );

            processDeploymentProgress(deploymentName, deploymentManager, deploymentProgressObject);
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    private void processDeploymentProgress(String appName, ObjectName deploymentManager, ObjectName deploymentProgressObject) throws Exception {
        if (deploymentProgressObject != null) {
            try {
                String state = waitForDeployToComplete(deploymentProgressObject, 200);
                if (state.equals("STATE_FAILED")) {
                    String[] targets = (String[]) connection.getAttribute(deploymentProgressObject, "FailedTargets");

                    RuntimeException[] exceptions = (RuntimeException[]) connection.invoke(
                            deploymentProgressObject, "getExceptions", new Object[]{targets[0]},
                            new String[]{String.class.getName()}
                    );

                    throw new DeploymentException("Deployment Failed on server: " + exceptions[0].getMessage(), exceptions[0]);
                }
            } finally {
                connection.invoke(deploymentManager, "removeDeploymentProgressObject",
                        new Object[]{appName}, new String[]{"java.lang.String"}
                );
            }
        }
    }

    private String waitForDeployToComplete(ObjectName progressObj, int timeToWaitInSecs) throws Exception {
        for (int i = 0; i < timeToWaitInSecs; i++) {
            String state = (String) connection.getAttribute(progressObj, "State");
            if ("STATE_COMPLETED".equals(state) || "STATE_FAILED".equals(state)) {
                return state;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        return "STATE_UNKNOWN";
    }

    /**
     * Sets the thread's context classloader to an instance of
     * {@link WebLogicJMXLibClassLoader}, that has the weblogic.jar from WL_HOME
     * as a codesource. The original context classloader of the thread is the
     * parent of the new classloader, and all classes to be loaded will be
     * delegated to the parent first, and then searched for in weblogic.jar (and
     * associated archives in the Manifest).
     *
     * We have to set the current thread's context classloader, instead of
     * relying on the "jmx.remote.protocol.provider.class.loader" key with an
     * associated value of an instance of {@link WebLogicJMXLibClassLoader} in
     * the environment specified to {@link JMXConnectorFactory}. Classes like
     * weblogic.jndi.WLInitialContextFactory will be loaded by the thread's
     * context classloader and not by the classloader used to load the JMX
     * provider.
     *
     * This method is preferably invoked as late as possible.
     */
    private void initWebLogicJMXLibClassLoader() {
        File wlHome = new File(configuration.getJmxClientJarPath());
        try {
            URL[] urls = {wlHome.toURI().toURL()};
            jmxLibraryClassLoader = new WebLogicJMXLibClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Thread.currentThread().setContextClassLoader(jmxLibraryClassLoader);
        } catch (MalformedURLException urlEx) {
            throw new RuntimeException(
                    "The constructed path to weblogic.jar appears to be invalid. Verify that you have access to this jar and it's dependencies.",
                    urlEx
            );
        }
    }

    /**
     * Initializes the connection to the Domain Runtime MBean Server
     *
     * @throws DeploymentException When a connection to the Domain Runtime MBean
     * Server could not be established.
     */
    private void createConnection() throws LifecycleException {
        if (connection != null) {
            return;
        }

        String protocol = configuration.getJmxProtocol();
        String hostname = configuration.getJmxHost();
        int portNum = configuration.getJmxPort();
        String domainRuntimeMBeanServerURL = "/jndi/weblogic.management.mbeanservers.domainruntime";

        try {
            JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname, portNum, domainRuntimeMBeanServerURL);

            Map<String, String> props = new HashMap<String, String>();
            props.put(Context.SECURITY_PRINCIPAL, configuration.getAdminUserName());
            props.put(Context.SECURITY_CREDENTIALS, configuration.getAdminPassword());
            props.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");

            connector = JMXConnectorFactory.connect(serviceURL, props);
            connection = connector.getMBeanServerConnection();
        } catch (IOException ioEx) {
            throw new LifecycleException("Failed to obtain a connection to the MBean Server.", ioEx);
        }
    }

    /**
     * Closes the connection to the Domain Runtime MBean Server.
     *
     * @throws LifecycleException
     */
    private void closeConnection() throws LifecycleException {
        try {
            if (connector != null) {
                connector.close();
            }
        } catch (IOException ioEx) {
            throw new LifecycleException("Failed to close the connection to the MBean Server.", ioEx);
        }
    }

    private void setConfiguredTrustStore() {
        stashInitialState();
        setupState();
    }

    /**
     * Stores the current state before attempting to change the classloaders,
     * and the system properties.
     */
    private void stashInitialState() {
        if (trustStorePath.get() == null && trustStorePassword.get() == null) {
            trustStorePath.set(System.getProperty("javax.net.ssl.trustStore"));
            trustStorePassword.set(System.getProperty("javax.net.ssl.trustStorePassword"));
        }
    }

    private void setupState() {
        if (configuration.isUseDemoTrust() || configuration.isUseCustomTrust() || configuration.isUseJavaStandardTrust()) {
            System.setProperty("javax.net.ssl.trustStore", configuration.getTrustStoreLocation());
            String trustStorePassword = configuration.getTrustStorePassword();
            // The default password for JKS truststores
            // usually need not be specified to read the CA certs.
            // But, if this was specified in arquillian.xml, we'll set it.
            if (trustStorePassword != null && !trustStorePassword.equals("")) {
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            }
        }
    }

    /**
     * Unsets the thread's context classloader to the original classloader.
     * We'll do this to ensure that Arquillian tests may run unaffected, if the
     * {@link WebLogicJMXLibClassLoader} were to interfere somehow.
     *
     * The truststore path and password is also reset to the original, to ensure
     * that Arquillian tests at the client, that use these properties, will run
     * without interference.
     *
     * This method is preferably invoked as soon as possible.
     */
    private void revertToInitialState() {
        if (trustStorePath.get() != null && trustStorePassword.get() != null) {
            System.setProperty("javax.net.ssl.trustStore", trustStorePath.get());
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword.get());
            trustStorePath.set(null);
            trustStorePassword.set(null);
        }
    }
}
