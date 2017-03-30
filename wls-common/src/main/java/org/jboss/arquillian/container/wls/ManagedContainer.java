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

import org.jboss.arquillian.container.spi.client.container.LifecycleException;

/**
 * A utility class for performing operations relevant to a WebLogic container managed by Arquillian.
 *
 * @author Vineet Reynolds
 */
public class ManagedContainer extends RemoteContainer {

    protected CommonManagedWebLogicConfiguration configuration;
    private WebLogicServerControl serverControl;
    protected boolean connectedToRunningServer = false;

    public ManagedContainer(CommonManagedWebLogicConfiguration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    /**
     * Starts the managed container process, and then delegates to the remote container implementation to discover
     * additional
     * container configuration via JMX.
     */
    @Override
    public void start() throws LifecycleException {
        serverControl = new WebLogicServerControl((CommonManagedWebLogicConfiguration) configuration);
        if (serverControl.isServerRunning()) {
            if (configuration.isAllowConnectingToRunningServer()) {
                connectedToRunningServer = true;
                super.start();
            } else {
                throw new LifecycleException("The server is already running! "
                    + "Managed containers does not support connecting to running server instances due to the "
                    + "possible harmful effect of connecting to the wrong server. Please stop server before running or "
                    + "change to another type of container.\n"
                    + "To disable this check and allow Arquillian to connect to a running server, "
                    + "set allowConnectingToRunningServer to true in the container configuration");
            }
        } else {
            serverControl.startServer();
            super.start();
        }
    }

    /**
     * Closes all resources consumed by the remote container client, and then stops the managed container process.
     */
    @Override
    public void stop() throws LifecycleException {
        try {
            super.stop();
        } finally {
            if (!connectedToRunningServer) {
                serverControl.stopServer();
            }
        }
    }
}
