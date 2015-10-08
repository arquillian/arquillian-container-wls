package org.jboss.arquillian.container.wls.managed.rest;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.wls.WebLogicManagedContainer;
import org.jboss.arquillian.container.wls.rest.RESTUtils;
import org.jboss.arquillian.container.wls.WebLogicServerControl;
import org.jboss.shrinkwrap.api.Archive;

import javax.ws.rs.client.Client;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:phil.zampino@oracle.com">Phil Zampino</a>
 */
public class ManagedContainer implements WebLogicManagedContainer {

  private static final Logger LOGGER = Logger.getLogger(ManagedContainer.class.getName());
  static {
    LOGGER.setLevel(Level.ALL);
  }

  private WebLogicManagedConfiguration config;

  private WebLogicServerControl serverControl;


  public ManagedContainer(WebLogicManagedConfiguration configuration) {
    config = configuration;
  }

  @Override
  public void start() throws LifecycleException {
    serverControl = new WebLogicServerControl(config);
    serverControl.startServer();
  }


  @Override
  public void stop() throws LifecycleException {
    // stopUsingREST();
    serverControl.stopServer();
  }

  private void stopUsingREST() {
//    URI applicationRestURI;
//    try {
//      URL adminUrl = new URL(config.getAdminUrl());
//      applicationRestURI = new URI(adminUrl.toURI().toString() +
//                                   "/management/weblogic/latest/domainRuntime/serverLifeCycleRuntimes/" +
//                                   config.getTarget() +
//                                   "/shutdown").normalize();
//    } catch (Exception e) {
//      throw new LifecycleException("Shutdown failed", e);
//    }
//
//    // This results in a HTTP 500 response, but it succeeds in shutting down the server
//    Client restClient = RESTUtils.getClient(config, LOGGER);
//    Invocation.Builder requestBuilder = restClient.target(applicationRestURI).request(MediaType.APPLICATION_JSON);
//    requestBuilder.post(Entity.entity("{}", MediaType.APPLICATION_JSON)); // Don't care about the response
  }

  /**
   * Deploy an application.
   *
   * @param archive The ShrinkWrap archive to deploy
   *
   * @return The metadata for the deployed application
   *
   * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
   */
  @SuppressWarnings("resource")
  public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
    return RESTUtils.deploy(config, LOGGER, archive);
  }


  /**
   * Undeploy an application.
   *
   * @param archive The ShrinkWrap archive to undeploy
   *
   * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
   */
  public void undeploy(Archive<?> archive) throws DeploymentException {
    RESTUtils.undeploy(config, LOGGER, archive);
  }

}
