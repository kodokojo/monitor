package io.kodokojo.monitor.service.marathon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.model.BrickConfiguration;
import io.kodokojo.commons.model.ProjectConfiguration;
import io.kodokojo.commons.service.BrickFactory;
import io.kodokojo.commons.service.BrickUrlFactory;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.commons.service.repository.ProjectFetcher;
import io.kodokojo.monitor.service.BrickStateLookup;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class MarathonBrickStateLookup implements BrickStateLookup {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonBrickStateLookup.class);

    private final MarathonConfig marathonConfig;

    private final ProjectFetcher projectFetcher;

    private final BrickFactory brickFactory;

    private final BrickUrlFactory brickUrlFactory;

    private final OkHttpClient httpClient;

    @Inject
    public MarathonBrickStateLookup(MarathonConfig marathonConfig, ProjectFetcher projectFetcher, BrickFactory brickFactory, BrickUrlFactory brickUrlFactory, OkHttpClient httpClient) {
        requireNonNull(marathonConfig, "marathonConfig must be defined.");
        requireNonNull(projectFetcher, "projectFetcher must be defined.");
        requireNonNull(brickFactory, "brickFactory must be defined.");
        requireNonNull(brickUrlFactory, "brickUrlFactory must be defined.");
        requireNonNull(httpClient, "httpClient must be defined.");
        this.projectFetcher = projectFetcher;
        this.brickFactory = brickFactory;
        this.brickUrlFactory = brickUrlFactory;
        this.marathonConfig = marathonConfig;
        this.httpClient = httpClient;
    }

    @Override
    public Set<BrickStateEvent> lookup() {

        Set<BrickStateEvent> res = new HashSet<>();

        String body = fetchMarathon();

        JsonParser parser = new JsonParser();

        if (StringUtils.isNotBlank(body)) {

            JsonObject root = (JsonObject) parser.parse(body);
            JsonArray apps = root.getAsJsonArray(APPS);

            for (JsonElement el : apps) {
                JsonObject app = (JsonObject) el;
                Optional<BrickStateEvent> brickStateEvent = processMarathonApplication(app);
                brickStateEvent.ifPresent(res::add);
            }
        }

        return res;
    }

    private Optional<BrickStateEvent> processMarathonApplication(JsonObject app) {
        if (app.has(ID) &&
                app.has(LABELS) &&
                app.getAsJsonObject(LABELS).has(MANAGED_BY_KODO_KOJO_HA) &&
                app.getAsJsonObject(LABELS).getAsJsonPrimitive(MANAGED_BY_KODO_KOJO_HA).getAsBoolean()
                ) {

            String id = app.getAsJsonPrimitive(ID).getAsString();
            String[] splitedId = id.substring(1).split(SEPARATOR);
            if (splitedId.length == NB_ELEMENT_EXPECTED) {
                String projectName = splitedId[PROJECT_NAME_INDEX];
                String brickName = splitedId[BRICK_NAME_INDEX];

                BrickStateEvent.State state = computeBrickState(app);

                return computeBrickStateEvent(projectName, brickName, state);
            }
        }
        return Optional.empty();
    }

    private Optional<BrickStateEvent> computeBrickStateEvent(String projectName, String brickName, BrickStateEvent.State state) {
        ProjectConfiguration projectConfiguration = projectFetcher.getProjectConfigurationByName(projectName);

        if (projectConfiguration == null) {
            return Optional.empty();
        }
        String projectConfigurationId = projectConfiguration.getIdentifier();
        String stackName = projectConfiguration.getDefaultStackConfiguration().getName();
        BrickConfiguration brickConfiguration = brickFactory.createBrick(brickName);
        String brickType = brickConfiguration.getType().name();
        String version = brickConfiguration.getVersion();
        String url = brickUrlFactory.forgeUrl(projectConfiguration.getName(), stackName, brickType, brickName);

        return Optional.of(new BrickStateEvent(projectConfigurationId, stackName, brickType, brickName, state, url, version));
    }

    private BrickStateEvent.State computeBrickState(JsonObject app) {
        int nbInstances = extractValue(app, INSTANCES);
        int tasksHealthy = extractValue(app, TASKS_HEALTHY);
        int tasksUnhealthy = extractValue(app, TASKS_UNHEALTHY);
        int tasksStaged = extractValue(app, TASKS_STAGED);
        int tasksRunning = extractValue(app, TASKS_RUNNING);


        BrickStateEvent.State state = BrickStateEvent.State.UNKNOWN;
        if (nbInstances > 0) {
            if (tasksRunning == nbInstances) {
                state = BrickStateEvent.State.RUNNING;

            } else if (tasksStaged > 0) {
                state = BrickStateEvent.State.STARTING;
            }

            if (tasksUnhealthy > 0) {
                state = BrickStateEvent.State.ONFAILURE;
            }
        } else {
            state = BrickStateEvent.State.STOPPED;
        }
        return state;
    }

    protected String fetchMarathon() {
        Request.Builder requestBuilder = new Request.Builder();
        Request.Builder builder = requestBuilder.url(marathonConfig.url() + V2_APPS_PATH).get();
        if (StringUtils.isNotBlank(marathonConfig.login())) {
            String basicAuthenticationValue = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", marathonConfig.login(), marathonConfig.password()).getBytes());
            builder.addHeader("Authorization", basicAuthenticationValue);
        }
        Request request = builder.build();
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            LOGGER.error("An error occur while trying to fetch Marathon application on url {}.", marathonConfig.url(), e);
        } finally {
            if (response != null) {
                IOUtils.closeQuietly(response);
            }
        }
        return null;
    }

    private int extractValue(JsonObject el, String attributeName) {
        assert el != null : "el must be defined";
        assert StringUtils.isNotBlank(attributeName) : "attributeName must be defined";

        if (el.has(attributeName)) {
            return el.getAsJsonPrimitive(attributeName).getAsInt();
        }
        return 0;
    }

    private static final String V2_APPS_PATH = "/v2/apps";

    private static final String TASKS_HEALTHY = "tasksHealthy";

    private static final String TASKS_UNHEALTHY = "tasksUnhealthy";

    private static final String TASKS_STAGED = "tasksStaged";

    private static final String TASKS_RUNNING = "tasksRunning";

    private static final String INSTANCES = "instances";

    private static final String APPS = "apps";

    private static final String LABELS = "labels";

    private static final String MANAGED_BY_KODO_KOJO_HA = "managedByKodoKojoHa";

    private static final String ID = "id";

    private static final String SEPARATOR = "/";

    private static final int NB_ELEMENT_EXPECTED = 2;

    private static final int PROJECT_NAME_INDEX = 0;

    private static final int BRICK_NAME_INDEX = 1;
}
