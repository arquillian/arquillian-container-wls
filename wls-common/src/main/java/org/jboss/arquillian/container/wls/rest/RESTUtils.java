package org.jboss.arquillian.container.wls.rest;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.media.multipart.*;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.wls.CommonWebLogicConfiguration;
import org.jboss.arquillian.container.wls.ShrinkWrapUtil;
import org.jboss.shrinkwrap.api.Archive;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:phil.zampino@oracle.com">Phil Zampino</a>
 */
public class RESTUtils {

    private static final String DOT = ".";

    private static final String COMMON_APP_URI = "/management/wls/latest/deployments/application";
    private static final String COMMON_APP_ID_URI = COMMON_APP_URI + "/id/";

    private static final String MONITORING_URI = "/management/tenant-monitoring/servers/";

    private static final String PURGE_PROGRESSS_OBJECTS_URI =
        "/management/weblogic/latest/domainRuntime/deploymentManager/purgeCompletedDeploymentProgressObjects";

    // Multi-part form data element constants
    private static final String MULTIPART_JSON_MODEL_PART_NAME = "model";
    private static final String MULTIPART_JSON_MODEL_NAME = "name";
    private static final String MULTIPART_JSON_MODEL_TARGETS = "targets";
    private static final String MULTIPART_JSON_MODEL_TYPE = "type";
    private static final String MULTIPART_JSON_MODEL_TYPE_VALUE = "application";
    private static final String MULTIPART_DEPLOYMENT_PART_NAME = "deployment";

    // HTTP header for REST requests
    private static final String HEADER_X_REQUESTED_BY_NAME = "X-Requested-By";
    private static final String HEADER_X_REQUESTED_BY_VALUE = "Arquillian WLS Container Adapter";

    // JSON response constants
    private static final String JSON_RESPONSE_BODY = "body";
    private static final String JSON_RESPONSE_ITEM = "item";
    private static final String JSON_RESPONSE_STATUS = "status";
    private static final String JSON_RESPONSE_STATUS_FAIL = "failed";
    private static final String JSON_RESPONSE_ERROR = "error";
    private static final String JSON_RESPONSE_ERRORS = "errors";
    private static final String JSON_RESPONSE_NAME = "name";
    private static final String JSON_RESPONSE_TARGETS = "targets";
    private static final String JSON_RESPONSE_STATE = "state";
    private static final String JSON_RESPONSE_SERVLETS = "servlets";
    private static final String JSON_RESPONSE_SERVLET_NAME = "servletName";
    private static final String JSON_RESPONSE_CONTEXT_PATH = "contextPath";

    private static final String JSON_RESPONSE_STATE_VALUE_RUNNING = "\"RUNNING\"";

    /**
     * Create an authenticating REST client for WebLogic Server interactions.
     *
     * @param config
     *     The client configuration
     * @param logger
     *     The Logger to use for client logging.
     *
     * @return A javax.ws.rs.Client based on the specified configuration
     */
    public static Client getClient(CommonWebLogicConfiguration config,
        Logger logger) {
        HttpAuthenticationFeature httpAuthFeature =
            HttpAuthenticationFeature.universalBuilder().credentialsForBasic(config.getAdminUserName(),
                config.getAdminPassword()).build();

        ClientBuilder restClientBuilder = ClientBuilder.newBuilder();

        restClientBuilder.register(CsrfProtectionFilter.class);
        restClientBuilder.register(httpAuthFeature);
        restClientBuilder.register(MultiPartFeature.class);
        restClientBuilder.register(JsonProcessingFeature.class);
        restClientBuilder.property(JsonGenerator.PRETTY_PRINTING, true); // format JSON content for readability

        // Register the logging filter only if configured to log the REST requests/responses (or the REST entities).
        // The REST entity logging is a finer level of detail that requires the logging filter, so it overrides the other
        // configuration property.
        if (config.isLogRESTMessages() || config.isLogRESTEntities()) {
            // Honor the verbose logging property to determine whether REST entities should be printed
            restClientBuilder.register(new LoggingFilter(logger, config.isLogRESTEntities()));
        }

        return restClientBuilder.build();
    }

    /**
     * Determine if the target server is running and ready.
     *
     * @param config
     *     The Arquillian configuration
     * @param logger
     *     A logger
     *
     * @return true IFF a connection can be made to the server, AND it's in the RUNNING state.
     */
    public static boolean isServerRunning(CommonWebLogicConfiguration config, Logger logger) {
        boolean isRunning = false;

        Client client = getClient(config, logger);
        WebTarget target = client.target(config.getAdminUrl() + MONITORING_URI + config.getTarget());

        try {
            Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                if (response.hasEntity()) {
                    JsonObject jsonResponse = response.readEntity(JsonObject.class);
                    if (jsonResponse.containsKey(JSON_RESPONSE_BODY)) {
                        JsonObject body = jsonResponse.getJsonObject(JSON_RESPONSE_BODY);
                        if (body.containsKey(JSON_RESPONSE_ITEM)) {
                            JsonObject item = body.getJsonObject(JSON_RESPONSE_ITEM);
                            if (item.containsKey(JSON_RESPONSE_STATE)) {
                                JsonValue state = item.get(JSON_RESPONSE_STATE);
                                isRunning = JSON_RESPONSE_STATE_VALUE_RUNNING.equals(state.toString().toUpperCase());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // This should only happen because the connection failed, which is expected in some cases,
            // and we'll just return false.
        } finally {
            client.close();
        }

        return isRunning;
    }

    /**
     * Invokes the REST management API to deploy an application.
     *
     * @param archive
     *     The ShrinkWrap archive to deploy
     *
     * @return The metadata for the deployed application
     *
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
     *     if the REST API call fails.
     */
    @SuppressWarnings("resource")
    public static ProtocolMetaData deploy(CommonWebLogicConfiguration config, Logger logger, Archive<?> archive)
        throws DeploymentException {

        // Export the ShrinkWrap archive to a temporary file
        File deploymentArchive = ShrinkWrapUtil.toFile(archive);

        // Create the JSON model for the deployment
        JsonObject model = Json.createObjectBuilder()
            .add(MULTIPART_JSON_MODEL_NAME, RESTUtils.getDeploymentName(archive))
            .add(MULTIPART_JSON_MODEL_TARGETS, Json.createArrayBuilder().add(config.getTarget()).build())
            .add(MULTIPART_JSON_MODEL_TYPE, MULTIPART_JSON_MODEL_TYPE_VALUE)
            .build();

        // Construct the multi-part request message
        FormDataMultiPart form = new FormDataMultiPart();
        form.field(MULTIPART_JSON_MODEL_PART_NAME, model, MediaType.APPLICATION_JSON_TYPE);
        form.bodyPart(new FileDataBodyPart(MULTIPART_DEPLOYMENT_PART_NAME, deploymentArchive,
            MediaType.APPLICATION_OCTET_STREAM_TYPE));

        // Construct the resource URL for deployment
        URL adminUrl;
        URI applicationRestURI;
        try {
            adminUrl = new URL(config.getAdminUrl());
            applicationRestURI = new URI(adminUrl.toURI().toString() + COMMON_APP_URI).normalize();
        } catch (Exception e) {
            throw new DeploymentException("Error constructing deployment resource URL.", e);
        }

        // Create and configure the REST client
        Client restClient = getClient(config, logger);

        // Prepare the deployment request
        Invocation.Builder requestBuilder =
            restClient.target(applicationRestURI).request(MediaType.APPLICATION_JSON_TYPE);
        requestBuilder.header(HEADER_X_REQUESTED_BY_NAME, HEADER_X_REQUESTED_BY_VALUE);

        // Post the deployment request
        Response response = requestBuilder.post(Entity.entity(form, form.getMediaType()));

        // Check the response status
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            if (response.hasEntity()) {
                // If we get anything but JSON back, there is a problem
                if (!MediaType.APPLICATION_JSON.equals(response.getMediaType().toString())) {
                    Response.StatusType status = response.getStatusInfo();
                    throw new DeploymentException("Deployment Failed: " + deploymentArchive.getName() +
                        " ; " + status.getStatusCode() + " " + status.getReasonPhrase());
                } else {
                    // Process the JSON response
                    JsonObject jsonResponse = response.readEntity(JsonObject.class);

                    if (jsonResponse.containsKey(JSON_RESPONSE_ITEM)) {
                        JsonObject jsonItem = jsonResponse.getJsonObject(JSON_RESPONSE_ITEM);

                        String responseItemStatus = jsonItem.getString(JSON_RESPONSE_STATUS);

                        // If the deployment failed, then throw a deployment exception with the error message from the response
                        if (JSON_RESPONSE_STATUS_FAIL.equals(responseItemStatus)) {
                            // Check the response item's "error" field
                            String exceptionMsg = jsonItem.getString(JSON_RESPONSE_ERROR);

                            // Must create the RuntimeException cause to satisfy CDI TCK expectations
                            throw new DeploymentException(exceptionMsg, new RuntimeException(exceptionMsg));
                        }
                    }
                }
            }
        }

        URI location = response.getLocation();
        if (location != null) {
            // Verify that the deployment succeeded by looking up the application's resource URI from the response
            response = restClient.target(location).request(MediaType.APPLICATION_JSON).get();
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new DeploymentException(getJSONResponseErrorMessage(response.readEntity(JsonObject.class)));
            }
        } else {
            throw new DeploymentException("Deployment failed! No resource location available from response.");
        }

        // Populate the Arquillian result metadata
        ProtocolMetaData metadata = new ProtocolMetaData();
        try {
            HTTPContext httpContext = new HTTPContext(adminUrl.getHost(), adminUrl.getPort());

            JsonObject jsonResponse = response.readEntity(JsonObject.class);
            if (jsonResponse.containsKey(JSON_RESPONSE_ITEM)) {
                JsonObject item = jsonResponse.getJsonObject(JSON_RESPONSE_ITEM);
                if (item.containsKey(JSON_RESPONSE_SERVLETS)) {
                    JsonArray servlets = item.getJsonArray(JSON_RESPONSE_SERVLETS);
                    for (JsonValue servlet : servlets) {
                        JsonObject servletJsonObject = (JsonObject) servlet;
                        if (servletJsonObject.containsKey(JSON_RESPONSE_SERVLET_NAME) && servletJsonObject.containsKey(
                            JSON_RESPONSE_CONTEXT_PATH)) {
                            httpContext.add(new Servlet(servletJsonObject.getString(JSON_RESPONSE_SERVLET_NAME),
                                servletJsonObject.getString(JSON_RESPONSE_CONTEXT_PATH)));
                        }
                    }
                }
            }

            metadata.addContext(httpContext);
        } catch (Exception e) {
            throw new DeploymentException("Failed to populate the ProtocolMetaData with the deployment details.", e);
        }

        boolean purgedCompletedProgressObjects = false;
        try {
            // Purge any completed deployment progress objects to avoid future failed deployments because of the server limit
            purgedCompletedProgressObjects = purgeCompletedDeploymentProgressObjects(restClient, config.getAdminUrl());
        } catch (Exception e) {
            //
        }

        if (!purgedCompletedProgressObjects) {
            logger.warning("Failed to purge deployment progress object(s).");
        }

        restClient.close();

        return metadata;
    }

    /**
     * Invokes the REST management API to undeploy the specified application.
     *
     * @param archive
     *     The ShrinkWrap archive to undeploy
     *
     * @throws org.jboss.arquillian.container.spi.client.container.DeploymentException
     *     if the REST API call fails.
     */
    public static void undeploy(CommonWebLogicConfiguration config, Logger logger, Archive<?> archive)
        throws DeploymentException {

        String deploymentName = RESTUtils.getDeploymentName(archive);
        Response response;
        try {
            Client restClient = getClient(config, logger);
            Invocation.Builder requestBuilder =
                restClient.target(new URI(config.getAdminUrl() + COMMON_APP_ID_URI + deploymentName)).request();
            response = requestBuilder.delete();
            restClient.close();
        } catch (URISyntaxException e) {
            throw new DeploymentException("Deployment failed", e);
        }

        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new DeploymentException(response.toString());
        }
    }

    private static Map<String, List<String>> getTargetErrors(JsonObject response) {
        return getTargetErrors(getTargets(response));
    }

    private static Map<String, List<String>> getTargetErrors(List<JsonObject> targets) {
        Map<String, List<String>> targetErrors = new HashMap<String, List<String>>();

        for (JsonObject target : targets) {
            JsonArray targetErrorsArray = target.getJsonArray(JSON_RESPONSE_ERRORS);

            List<String> errorMessages = new ArrayList<String>();
            for (JsonValue targetError : targetErrorsArray) {
                errorMessages.add(targetError.toString());
            }

            targetErrors.put(target.getString(JSON_RESPONSE_NAME), errorMessages);
        }

        return targetErrors;
    }

    private static Map<String, String> getTargetStatuses(List<JsonObject> targets) {
        Map<String, String> targetStatuses = new HashMap<String, String>();

        for (JsonObject target : targets) {
            targetStatuses.put(target.getString(JSON_RESPONSE_NAME), target.getString(JSON_RESPONSE_STATUS));
        }

        return targetStatuses;
    }

    private static List<JsonObject> getTargets(JsonObject response) {
        List<JsonObject> targetJsonObjects = new ArrayList<JsonObject>();

        JsonObject item = response.getJsonObject(JSON_RESPONSE_ITEM);
        if (item != null) {
            JsonArray targets = item.getJsonArray(JSON_RESPONSE_TARGETS);
            for (JsonValue target : targets) {
                JsonObject targetJsonObject = (JsonObject) target;
                targetJsonObjects.add(targetJsonObject);
            }
        }

        return targetJsonObjects;
    }

    /**
     * The deployment error message from the specified JSON response object.
     *
     * @param jsonResponse
     *     The JSONResponse object, which may have an "error" message.
     *
     * @return The "error" message value or null.
     */
    private static String getJSONResponseErrorMessage(JsonObject jsonResponse) {
        String responseErrorMessage = null;

        if (jsonResponse.containsKey(JSON_RESPONSE_ITEM)) {
            JsonObject item = jsonResponse.getJsonObject(JSON_RESPONSE_ITEM);
            if (item.containsKey(JSON_RESPONSE_ERROR)) {
                responseErrorMessage = item.getString(JSON_RESPONSE_ERROR);
            }
        }

        return responseErrorMessage;
    }

    /**
     * Purge completed deployment progress objects.
     * This is necessary because WebLogic Server has a configurable limit on the number of these objects allowed to exist
     * at any given time. While there is a periodic purge performed automatically, it's not performed frequently enough
     * to prevent deployment errors when the number of Arquillian tests being run exceeds the configured limit.
     * <p>
     * This method is intended to be invoked after each deployment to clear any of these objects left from completed
     * deployments.
     *
     * @param restClient
     *     The client with which to communicate the request to purge the progress objects.
     * @param adminURL
     *     The URL of the admin server, from which the resource URI will be constructed.
     *
     * @throws Exception
     */
    private static boolean purgeCompletedDeploymentProgressObjects(Client restClient, String adminURL) throws Exception {
        final String resourceURI = adminURL + PURGE_PROGRESSS_OBJECTS_URI;
        Invocation.Builder requestBuilder = restClient.target(new URI(resourceURI)).request();
        Response response = requestBuilder.post(null);
        return (response.getStatus() == Response.Status.OK.getStatusCode());
    }

    /**
     * Determine the WebLogic deployment name from the specified archive.
     *
     * @param archive
     *     A ShrinkWrap archive
     *
     * @return The deployment name associated with the specified archive
     */
    public static String getDeploymentName(Archive<?> archive) {
        String archiveFilename = archive.getName();
        int indexOfDot = archiveFilename.indexOf(DOT);
        if (indexOfDot != -1) {
            return archiveFilename.substring(0, indexOfDot);
        }
        return archiveFilename;
    }
}
