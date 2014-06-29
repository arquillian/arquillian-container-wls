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

/**
 * A utility class for performing operations relevant to a remote WebLogic container used by Arquillian.
 * <p>
 * Relies completely on the JMX client to perform deployments. WLS 12.1.2 containers and higher are encouraged to use
 * this class.  Will NOT work on WLS 12.1.1 and earlier.
 * 
 * @author Vineet Reynolds
 *
 */
public class FullJMXRemoteContainer {

    private WebLogicJMXClient jmxClient;
    private CommonWebLogicConfiguration configuration;

    public FullJMXRemoteContainer(CommonWebLogicConfiguration configuration) {
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
     * Wraps the operation of calling the Domain Runtime MBean Server via JMX to deploy an application
     *
     * @param archive The ShrinkWrap archive to deploy
     * @return The metadata for the deployed application
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException 
     */
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return jmxClient.deploy(getDeploymentName(archive), ShrinkWrapUtil.toFile(archive));
    }

    /**
     * Wraps the operation of calling the Domain Runtime MBean Server via JMX to undeploy an application
     *
     * @param archive The ShrinkWrap archive to undeploy
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
     */
    public void undeploy(Archive<?> archive) throws DeploymentException {
        jmxClient.undeploy(getDeploymentName(archive));
    }
    
    /**
     * Stops the JMX client.
     *
     * @throws org.jboss.arquillian.container.spi.client.container.LifecycleException When there is failure in closing the JMX connection.
     */
    public void stop() throws LifecycleException {
        jmxClient.close();
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
