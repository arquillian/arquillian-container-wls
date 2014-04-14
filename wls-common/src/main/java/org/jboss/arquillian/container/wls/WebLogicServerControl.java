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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.container.LifecycleException;

/**
 * The process controller for the managed WebLogic container.
 * Starts and shuts down the WebLogic container through shell scripts in the WebLogic domain directory.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicServerControl {

    private static final Logger logger = Logger.getLogger(WebLogicServerControl.class.getName());
    private CommonManagedWebLogicConfiguration configuration;

    public WebLogicServerControl(CommonManagedWebLogicConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Start an AdminServer instance.
     * 
     * @throws LifecycleException when there is a failure starting the WLS instance. 
     */
    public void startServer() throws LifecycleException {
        new StartupAdminServerCommand().execute();
    }

    /**
     * Stops a running AdminServer instance.
     * 
     * @throws LifecycleException when there is a failure stopping the WLS instance.
     */
    public void stopServer() throws LifecycleException {
        new ShutdownAdminServerCommand().execute();
    }

    /**
     * Attempts to establish a socket to the admin server's listen address and port to determine whether it is running or not.
     * 
     * @return whether the Admin Server is running or not
     */
    public boolean isServerRunning() {
        Socket socket = null;
        try {
            socket = new Socket(configuration.getAdminListenAddress(), configuration.getAdminListenPort());
        } catch (Exception ignored) {
            return false;
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioEx) {
                throw new RuntimeException("Failed to close socket", ioEx);
            }
        }
        return true;
    }

    /**
     * An abstract command object to execute shell commands that control the server lifecycle.
     * 
     * @author Vineet Reynolds
     * 
     */
    private abstract class ShellCommand {
        
        /**
         * Executes the shell command.represented by this command object.
         * 
         * @throws LifecycleException when there is a failure during execution of the shell command
         */
        protected abstract void execute() throws LifecycleException;

        /**
         * Returns the shell script to be executed by the command. The return value is eventually passed as a parameter to the
         * shell interpreter of the executing environment.
         * 
         * @return The script to be executed.
         */
        protected abstract String getScript();

        /**
         * Returns the shell interpret command and a list of arguments to be passed to the interpreter. The list of arguments
         * includes the path to the script file to be executed.
         * 
         * @return A {@link List} containing the shell script interpreter for the OS environment and the script to be executed.
         */
        protected final List<String> getCommand() {
            List<String> command = new ArrayList<String>();
            command.addAll(getShellInterpreter());
            command.add(getScript());
            return command;
        }

        /**
         * Returns the name of the executable for the shell interpreter and parameters to allow for executing a shell script
         * file. The executable is assumed to be present in any of the directories in the PATH. An absolute path to the shell
         * interpreter will not be returned.
         * 
         * @return A {@link List} of strings representing the name of the executable for the shell interpreter, and a parameter
         *         to allow a shell script file to be passed as an argument to the shell interpreter.
         */
        private List<String> getShellInterpreter() {
            List<String> shellCommands = new ArrayList<String>();
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows")) {
                shellCommands.add("cmd.exe");
                shellCommands.add("/c");
            } else {
                String shell = System.getenv("SHELL");
                if (shell == null) {
                    shellCommands.add("sh");
                    shellCommands.add("-c");
                } else {
                    shellCommands.add(shell);
                    shellCommands.add("-c");
                }
            }
            return shellCommands;
        }

    }

    /**
     * The command implementation for starting a new WebLogic Server process.
     * Execute the startWebLogic script in the domainHome/bin directory by default.
     * 
     * @author Vineet Reynolds
     *
     */
    private class StartupAdminServerCommand extends ShellCommand {

        /**
         * Starts a new admin server instance by running the startup script in the shell. The script is executed using the WLS
         * domain home as the working directory of the shell. MW_HOME is passed to the shell environment for use by the script.
         * 
         * If the AdminServer is not running by the timeout specified in the Arquillian configuration, the forked process is
         * killed. This may or may not kill the JVM child-process, depending on the OS.
         */
        @Override
        public void execute() throws LifecycleException {
            Process process = null;
            try {
                List<String> command = getCommand();
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(new File(configuration.getDomainDirectory()));
                builder.environment().put("MW_HOME", configuration.getMiddlewareHome());
                String jvmOptions = configuration.getJvmOptions();
                if (jvmOptions != null && jvmOptions.length() > 0) {
                    builder.environment().put("JAVA_OPTIONS", configuration.getJvmOptions());
                }
                builder.redirectErrorStream(true);
                process = builder.start();
                Thread consoleConsumer = new Thread(new ConsoleConsumer(process, configuration.isOutputToConsole()));
                consoleConsumer.setDaemon(true);
                consoleConsumer.start();
                final int timeout = configuration.getTimeout();
                long start = System.currentTimeMillis() / 1000;
                long now = start;
                boolean serverAvailable = false;
                while ((now - start) < timeout && serverAvailable == false) {
                    serverAvailable = isServerRunning();
                    if (!serverAvailable) {
                        if (processHasDied(process)) {
                            break;
                        }
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException interruptedEx) {
                            logger.log(Level.INFO, "Container startup interrupted");
                            throw interruptedEx;
                        }
                        now = System.currentTimeMillis() / 1000;
                    }
                }
                if (!serverAvailable) {
                    process.destroy();
                    throw new TimeoutException(String.format("The startup script could not complete in %d seconds.",
                            configuration.getTimeout()));
                }
                logger.log(Level.INFO, "Started WebLogic Server.");
                return;
            } catch (Exception ex) {
                throw new LifecycleException("Container startup failed.", ex);
            }
        }

        @Override
        protected String getScript() {
            return configuration.getStartServerScript();
        }

        private boolean processHasDied(Process process) {
            try {
                process.exitValue();
                return true;
            } catch (IllegalThreadStateException illegalEx) {
                return false;
            }
        }

    }

    /**
     * The command implementation for stopping a running WLS AdminServer instance.
     * Execute the stopWebLogic script in the domainHome/bin directory by default.
     * 
     * @author Vineet Reynolds
     *
     */
    private class ShutdownAdminServerCommand extends ShellCommand {

        /**
         * Stops an existing AdminServer instance by running the shutdown script in the shell. The script is executed using the
         * WLS domain home as the working directory of the shell. MW_HOME is passed to the shell environment for use by the
         * script.
         * 
         * No arguments are passed to the shutdown script. This assumes that the script is configured to connect to the
         * AdminServer and shut it down without passing in credentials as shell parameters.
         */
        @Override
        public void execute() throws LifecycleException {
            Process process = null;
            try {
                List<String> command = getCommand();
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(new File(configuration.getDomainDirectory()));
                builder.environment().put("MW_HOME", configuration.getMiddlewareHome());
                builder.redirectErrorStream(true);
                process = builder.start();
                Thread consoleConsumer = new Thread(new ConsoleConsumer(process, configuration.isOutputToConsole()));
                consoleConsumer.setDaemon(true);
                consoleConsumer.start();
                final int timeout = configuration.getTimeout();
                long start = System.currentTimeMillis() / 1000;
                long now = start;
                boolean serverAvailable = true;
                while ((now - start) < timeout && serverAvailable == true) {
                    serverAvailable = isServerRunning();
                    if (serverAvailable) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException interruptedEx) {
                            logger.log(Level.INFO, "Container shutdown interrupted");
                            throw interruptedEx;
                        }
                        now = System.currentTimeMillis() / 1000;
                    } else {
                        break;
                    }
                }
                if (serverAvailable) {
                    process.destroy();
                    throw new TimeoutException(String.format("The shutdown script could not complete in %d seconds.",
                            configuration.getTimeout()));
                }
                logger.log(Level.INFO, "Stopped WebLogic Server.");
                return;
            } catch (Exception ex) {
                throw new LifecycleException("Container shutdown failed.", ex);
            }
        }

        @Override
        protected String getScript() {
            return configuration.getStopServerScript();
        }

    }

    /**
     * A helper class to read the output stream of the scripts executed by {@link WebLogicServerControl}.
     * Writes the contents of the stream to the console if configured to do so.
     * 
     * @author Vineet Reynolds
     *
     */
    private class ConsoleConsumer implements Runnable {

        private Process process;
        private boolean writeOutput;

        private ConsoleConsumer(Process process, boolean writeOutput) {
            this.process = process;
            this.writeOutput = writeOutput;
        }

        public void run() {
            final InputStream stream = process.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    if (writeOutput) {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

    }

}