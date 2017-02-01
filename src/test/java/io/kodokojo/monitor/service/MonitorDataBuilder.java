package io.kodokojo.monitor.service;

import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.model.BrickType;
import io.kodokojo.commons.model.ProjectConfiguration;
import io.kodokojo.commons.model.ProjectConfigurationBuilder;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public interface MonitorDataBuilder {

    default String fetchAllMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/allApps.json");
    }

    default String fetchJenkinsHealthyMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/jenkins_healthy.json");
    }

    default String fetchJenkinsFailMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/jenkins_fail.json");
    }

    default String fetchJenkinsRunningMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/jenkins_running.json");
    }

    default String fetchJenkinsStageMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/jenkins_staged.json");
    }

    default String fetchJenkinsStoppedMarathonAppsResponse() {
        return fetchJsonFromFile("marathon/jenkins_stop.json");
    }

    default String fetchJsonFromFile(String fileName) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        try {
            String content = IOUtils.toString(inputStream);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    default Set<BrickStateEvent> aBrickStateEvents() {
        Set<BrickStateEvent> initialBrickStateEvents = new HashSet<>();
        String projectConfigurationIdentifier = "5678";
        String stackName = "build-A";
        initialBrickStateEvents.add(new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.RUNNING, "1.651.3"));
        initialBrickStateEvents.add(new BrickStateEvent("0123ss", stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.STOPPED, "1.651.3"));
        initialBrickStateEvents.add(new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.ONFAILURE, "2.13"));
        initialBrickStateEvents.add(new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0"));
        return  initialBrickStateEvents;
    }

}
