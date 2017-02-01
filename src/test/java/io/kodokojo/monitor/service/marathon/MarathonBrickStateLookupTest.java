package io.kodokojo.monitor.service.marathon;

import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.model.BrickType;
import io.kodokojo.commons.service.DefaultBrickFactory;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.commons.service.repository.ProjectFetcher;
import io.kodokojo.monitor.service.BrickStateLookup;
import io.kodokojo.monitor.service.MonitorDataBuilder;
import io.kodokojo.test.DataBuilder;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MarathonBrickStateLookupTest implements MonitorDataBuilder, DataBuilder {

    private MarathonConfig marathonConfig;

    private ProjectFetcher projectFetcher;

    private OkHttpClient httpClient;

    @Before
    public void setup() {
        marathonConfig = aMarathonConfig();
        projectFetcher = mock(ProjectFetcher.class);

        when(projectFetcher.getProjectConfigurationByName("myproject")).thenReturn(aProjectConfiguration());

        httpClient = mock(OkHttpClient.class);
    }

    @Test
    public void lookup_simple_healthy_application_test() {
        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchJenkinsHealthyMarathonAppsResponse();
            }
        };

        lookup_simple_application_test(brickStateLookup, BrickStateEvent.State.RUNNING);
    }

    @Test
    public void lookup_simple_fail_application_test() {

        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchJenkinsFailMarathonAppsResponse();
            }
        };

        lookup_simple_application_test(brickStateLookup, BrickStateEvent.State.ONFAILURE);

    }


    @Test
    public void lookup_simple_running_application_test() {

        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchJenkinsRunningMarathonAppsResponse();
            }
        };

        lookup_simple_application_test(brickStateLookup, BrickStateEvent.State.RUNNING);

    }

    @Test
    public void lookup_simple_stopped_application_test() {

        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchJenkinsStoppedMarathonAppsResponse();
            }
        };

        lookup_simple_application_test(brickStateLookup, BrickStateEvent.State.STOPPED);

    }

    @Test
    public void lookup_simple_staged_application_test() {

        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchJenkinsStageMarathonAppsResponse();
            }
        };

        lookup_simple_application_test(brickStateLookup, BrickStateEvent.State.STARTING);

    }

    @Test
    public void acceptance_test() {

        BrickStateLookup brickStateLookup = new MarathonBrickStateLookup(marathonConfig, projectFetcher, new DefaultBrickFactory(), httpClient) {
            @Override
            protected String fetchMarathon() {
                return fetchAllMarathonAppsResponse();
            }
        };

        Set<BrickStateEvent> brickStateEvents = brickStateLookup.lookup();

        //  Then

        Collection<BrickStateEvent> expectedStates = aBrickStateEvents();
        assertThat(brickStateEvents).containsOnlyElementsOf(expectedStates);

    }


    private void lookup_simple_application_test(BrickStateLookup brickStateLookup, BrickStateEvent.State expectedState) {
        //  When

        Set<BrickStateEvent> brickStateEvents = brickStateLookup.lookup();

        //  Then

        BrickStateEvent expectedBrickStateEvent = new BrickStateEvent("5678", "build-A", BrickType.CI.name(), "jenkins", expectedState, "1.651.3");

        assertThat(brickStateEvents).contains(expectedBrickStateEvent);
        assertThat(brickStateEvents.size()).isEqualTo(1);

    }

}
