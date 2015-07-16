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
package org.jboss.arquillian.container.wls.managed_10_3;

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
 * The process controller for the WebLogic 12.1.x container.
 * Starts and shuts down the WebLogic container through shell scripts in the WebLogic domain.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicServerControl {

    private static final Logger logger = Logger.getLogger(WebLogicServerControl.class.getName());
    private WebLogicManagedConfiguration configuration;

    public WebLogicServerControl(WebLogicManagedConfiguration configuration) {
        this.configuration = configuration;
    }

    public void start() throws LifecycleException {
        new StartupAdminServerCommand().execute();
    }

    public void stop() throws LifecycleException {
        new ShutdownAdminServerCommand().execute();
    }

    public boolean isServerRunning() {
        Socket socket = null;
        try {
            socket = new Socket("localhost", 7001);
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

    private abstract class ShellCommand {
        protected abstract void execute() throws LifecycleException;

        protected abstract String getScript();

        protected final List<String> getCommand() {
            List<String> command = new ArrayList<String>();
            String domainDir = configuration.getDomainDirectory();
            String executable = (domainDir.endsWith(File.separator) ? domainDir : domainDir + File.separator) + "bin"
                    + File.separator + getScript();
            command.addAll(getShellInterpreter());
            command.add(executable);
            return command;
        }

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

    private class StartupAdminServerCommand extends ShellCommand {

        @Override
        public void execute() throws LifecycleException {
            Process process = null;
            try {
                List<String> command = getCommand();
                ProcessBuilder builder = new ProcessBuilder(command);
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
                        Thread.sleep(1000L);
                        now = System.currentTimeMillis() / 1000;
                    }
                }
                if (!serverAvailable) {
                    process.destroy();
                    throw new TimeoutException(String.format("The startup script could not complete in [%d] seconds.",
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
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows")) {
                return "startWebLogic.cmd";
            } else {
                return "startWebLogic.sh";
            }
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

    private class ShutdownAdminServerCommand extends ShellCommand {

        @Override
        public void execute() throws LifecycleException {
            Process process = null;
            try {
                List<String> command = new ShutdownAdminServerCommand().getCommand();
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                process = builder.start();
                Thread consoleConsumer = new Thread(new ConsoleConsumer(process, configuration.isOutputToConsole()));
                consoleConsumer.setDaemon(true);
                consoleConsumer.start();
                while (isServerRunning()) {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                        logger.log(Level.INFO, "Container shutdown interrupted");
                        throw e;
                    }
                }
                logger.log(Level.INFO, "Stopped WebLogic Server.");
                return;
            } catch (Exception ex) {
                throw new LifecycleException("Container shutdown failed.", ex);
            }
        }

        @Override
        protected String getScript() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows")) {
                return "stopWebLogic.cmd";
            } else {
                return "stopWebLogic.sh";
            }
        }

    }

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