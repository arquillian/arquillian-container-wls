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
package org.jboss.arquillian.container.wls.jmx;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.wls.CommonWebLogicConfiguration;
import org.jboss.arquillian.container.wls.ShrinkWrapUtil;
import org.jboss.arquillian.container.wls.WebLogicJMXClient;
import org.jboss.shrinkwrap.api.Archive;

import java.io.File;

/**
 * A utility class for performing operations relevant to a remote WebLogic container used by Arquillian.
 * Relies completely on the JMX client to perform deployments. WLS 12.1.2 containers and higher are encouraged to use
 * this class.
 * 
 * @author Vineet Reynolds
 *
 */
public class RemoteContainer {

    private WebLogicJMXClient jmxClient;
    private CommonWebLogicConfiguration configuration;

    public RemoteContainer(CommonWebLogicConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Starts a JMX client to read container metadata from the Domain Runtime MBean Server.
     * 
     * @throws org.jboss.arquillian.container.spi.client.container.LifecycleException When a connection cannot be created to the MBean Server.
     */
    public void start() throws LifecycleException {
        jmxClient = new WebLogicJMXClient(configuration);
    }

    /**
     * Stops the JMX client.
     *
     * @throws org.jboss.arquillian.container.spi.client.container.LifecycleException When there is failure in closing the JMX connection.
     */
    public void stop() throws LifecycleException {
        jmxClient.close();
    }

    /**
     * Wraps the operation of forking a weblogic.Deployer process to deploy an application.
     *
     * @param archive The ShrinkWrap archive to deploy
     * @return The metadata for the deployed application
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException When forking of weblogic.Deployer fails, or when interaction with the forked process fails,
     *         or when details of the deployment cannot be obtained from the Domain Runtime MBean Server.
     */
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String deploymentName = getDeploymentName(archive);
        File deploymentArchive = ShrinkWrapUtil.toFile(archive);

        ProtocolMetaData metadata = jmxClient.deploy(deploymentName, deploymentArchive);
        return metadata;
    }

    /**
     * Wraps the operation of forking a weblogic.Deployer process to undeploy an application.
     *
     * @param archive The ShrinkWrap archive to undeploy
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException When forking of weblogic.Deployer fails, or when interaction with the forked process fails,
     *         or when undeployment cannot be confirmed.
     */
    public void undeploy(Archive<?> archive) throws DeploymentException {
        // Undeploy the application
        String deploymentName = getDeploymentName(archive);

        // Verify the undeployment from the Domain Runtime MBean Server.
        jmxClient.undeploy(deploymentName);
    }

    private String getDeploymentName(Archive<?> archive) {
        String archiveFilename = archive.getName();
        int indexOfDot = archiveFilename.indexOf(".");
        if (indexOfDot != -1) {
            return archiveFilename.substring(0, indexOfDot);
        }
        return archiveFilename;
    }

}
