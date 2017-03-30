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

import org.jboss.arquillian.container.spi.ConfigurationException;

/**
 * Arquillian properties for the managed WebLogic container. Properties derived from the
 * {@link CommonWebLogicConfiguration} class are added to, here.
 *
 * @author Vineet Reynolds
 */
public class CommonManagedWebLogicConfiguration extends CommonWebLogicConfiguration {

    private static final String DEFAULT_WIN_STARTUP_SCRIPT = "bin\\\\startWebLogic.cmd";
    private static final String DEFAULT_LINUX_STARTUP_SCRIPT = "./bin/startWebLogic.sh";
    private static final String DEFAULT_WIN_SHUTDOWN_SCRIPT = "bin\\\\stopWebLogic.cmd";
    private static final String DEFAULT_LINUX_SHUTDOWN_SCRIPT = "./bin/stopWebLogic.sh";

    private String middlewareHome = System.getenv("MW_HOME");
    private String domainDirectory;
    private String jvmOptions;
    private int timeout = 60;
    private boolean outputToConsole = false;
    private boolean allowConnectingToRunningServer = false;
    private String startServerScript;
    private String stopServerScript;

    public CommonManagedWebLogicConfiguration() {
        super();
    }

    @Override
    public void validate() throws ConfigurationException {
        Validate.directoryExists(middlewareHome,
            "The middlewareHome resolved to " + middlewareHome +
                " and could not be located. Verify the property in arquillian.xml");

        Validate.directoryExists(domainDirectory,
            "The domainDirectory resolved to " + domainDirectory +
                " and could not be located. Verify the property in arquillian.xml");

        if (startServerScript != null && startServerScript.length() > 0) {
            Validate.isValidFile(startServerScript, "The startServerScript resolved to " + startServerScript
                + " and could not be located. Verify the property in arquillian.xml");
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows")) {
                startServerScript = DEFAULT_WIN_STARTUP_SCRIPT;
            } else {
                startServerScript = DEFAULT_LINUX_STARTUP_SCRIPT;
            }
        }
        if (stopServerScript != null && stopServerScript.length() > 0) {
            Validate.isValidFile(stopServerScript, "The stopServerScript resolved to " + stopServerScript
                + " and could not be located. Verify the property in arquillian.xml");
        } else {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows")) {
                stopServerScript = DEFAULT_WIN_SHUTDOWN_SCRIPT;
            } else {
                stopServerScript = DEFAULT_LINUX_SHUTDOWN_SCRIPT;
            }
        }
        super.validate();
    }

    public String getMiddlewareHome() {
        return middlewareHome;
    }

    /**
     * @param middlewareHome
     *     The directory representing the Oracle Middleware Home. Defaults to the MW_HOME environment
     *     variable.
     */
    public void setMiddlewareHome(String middlewareHome) {
        this.middlewareHome = middlewareHome;
    }

    public String getDomainDirectory() {
        return domainDirectory;
    }

    /**
     * @param domainDirectory
     *     The WebLogic Server domain directory.
     */
    public void setDomainDirectory(String domainDirectory) {
        this.domainDirectory = domainDirectory;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    /**
     * @param jvmOptions
     *     Used to set the JAVA_OPTIONS environment variable for the shell environment. The environment variable
     *     can then be used in the script used to start the server.
     */
    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout
     *     The duration in number of seconds, by which the startup and shutdown script should complete. Defaults to
     *     60. If the server is not detected to have started or shutdown by this interval, the container action is deemed
     *     to
     *     have failed.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public boolean isOutputToConsole() {
        return outputToConsole;
    }

    /**
     * @param outputToConsole
     *     Whether the output from the execution of the shell scripts should be logged to the console.
     */
    public void setOutputToConsole(boolean outputToConsole) {
        this.outputToConsole = outputToConsole;
    }

    public boolean isAllowConnectingToRunningServer() {
        return allowConnectingToRunningServer;
    }

    /**
     * @param allowConnectingToRunningServer
     *     Whether Arquillian should be allowed to connect and run tests in an already running
     *     WebLogic Server instance.
     */
    public void setAllowConnectingToRunningServer(boolean allowConnectingToRunningServer) {
        this.allowConnectingToRunningServer = allowConnectingToRunningServer;
    }

    public String getStartServerScript() {
        return startServerScript;
    }

    /**
     * @param startServerScript
     *     The script used to start the WebLogic Server instance. Defaults to the startWebLogic script in
     *     the bin sub-directory of the domain home.
     */
    public void setStartServerScript(String startServerScript) {
        this.startServerScript = startServerScript;
    }

    public String getStopServerScript() {
        return stopServerScript;
    }

    /**
     * @param stopServerScript
     *     The script used to stop the WebLogic Server instance. Defaults to the stopWebLogic script in the
     *     bin sub-directory of the domain home.
     */
    public void setStopServerScript(String stopServerScript) {
        this.stopServerScript = stopServerScript;
    }
}
