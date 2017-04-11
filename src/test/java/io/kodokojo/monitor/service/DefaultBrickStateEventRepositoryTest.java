package io.kodokojo.monitor.service;

import io.kodokojo.commons.model.BrickType;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultBrickStateEventRepositoryTest implements MonitorDataBuilder {

    @Test
    public void init_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();

        Set<BrickStateEvent> initialBrickStateEvents = aBrickStateEvents();
        //  when
        Set<BrickStateEvent> brickStateEvents = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        //  then

        assertThat(brickStateEvents).containsOnlyElementsOf(initialBrickStateEvents);
    }

    @Test
    public void add_one_brick_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();
        Set<BrickStateEvent> initialBrickStateEvents = aBrickStateEvents();
        Set<BrickStateEvent> brickStateEvents = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        Set<BrickStateEvent> addBrickStateEvents = new HashSet<>(initialBrickStateEvents);
        BrickStateEvent brickStateEvent = new BrickStateEvent("91011", "build-A", BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.RUNNING , "1.651.3");
        addBrickStateEvents.add(brickStateEvent);

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(addBrickStateEvents);

        //  then

        assertThat(result).containsOnly(brickStateEvent);
    }

    @Test
    public void add_two_brick_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();
        Set<BrickStateEvent> initialBrickStateEvents = aBrickStateEvents();
        Set<BrickStateEvent> brickStateEvents = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        Set<BrickStateEvent> addBrickStateEvents = new HashSet<>(initialBrickStateEvents);
        BrickStateEvent brickStateEvent = new BrickStateEvent("91011", "build-A", BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.RUNNING , "1.651.3");
        BrickStateEvent secondBrickStateEvent = new BrickStateEvent("91011", "build-A", BrickType.CI.name(), "jenkins", BrickStateEvent.State.RUNNING , "1.651.3");
        addBrickStateEvents.add(brickStateEvent);
        addBrickStateEvents.add(secondBrickStateEvent);

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(addBrickStateEvents);

        //  then

        assertThat(result).containsOnly(brickStateEvent, secondBrickStateEvent);
    }

    @Test
    public void remove_one_brick_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();
        String projectConfigurationIdentifier = "5678";
        String stackName = "build-A";
        Set<BrickStateEvent> initialBrickStateEvents = new HashSet<>();
        BrickStateEvent jenkins = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.STOPPED, "1.651.3");
        BrickStateEvent nexus = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.ONFAILURE, "2.13");
        BrickStateEvent gitlab = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0");

        initialBrickStateEvents.add(jenkins);
        initialBrickStateEvents.add(nexus);
        initialBrickStateEvents.add(gitlab);

        Set<BrickStateEvent> brickStateEvents = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        Set<BrickStateEvent> removeBrickStateEvents = new HashSet<>();
        removeBrickStateEvents.add(jenkins);
        removeBrickStateEvents.add(nexus);

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(removeBrickStateEvents);

        //  then
        BrickStateEvent gitlabUnknow = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STOPPED, "8.13.0-ce.0");
        assertThat(result).containsOnly(gitlabUnknow);
    }

    @Test
    public void remove_one_brick_then_come_back_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();
        String projectConfigurationIdentifier = "5678";
        String stackName = "build-A";
        Set<BrickStateEvent> initialBrickStateEvents = new HashSet<>();
        BrickStateEvent jenkins = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.STOPPED, "1.651.3");
        BrickStateEvent nexus = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.ONFAILURE, "2.13");
        BrickStateEvent gitlab = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0");

        initialBrickStateEvents.add(jenkins);
        initialBrickStateEvents.add(nexus);
        initialBrickStateEvents.add(gitlab);

        Set<BrickStateEvent> brickStateEvents = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        Set<BrickStateEvent> removeBrickStateEvents = new HashSet<>();
        removeBrickStateEvents.add(jenkins);
        removeBrickStateEvents.add(nexus);

        Set<BrickStateEvent> intermediaire = defaultBrickStateEventRepository.compareAndUpdate(removeBrickStateEvents);
        BrickStateEvent gitlabUnknow = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STOPPED, "8.13.0-ce.0");

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        //  then
        assertThat(intermediaire).containsOnly(gitlabUnknow);
        assertThat(result).containsOnly(gitlab);
    }



    @Test
    public void update_one_brick_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();

        String projectConfigurationIdentifier = "5678";
        String stackName = "build-A";
        Set<BrickStateEvent> initialBrickStateEvents = new HashSet<>();
        BrickStateEvent jenkins = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.STOPPED, "1.651.3");
        BrickStateEvent nexus = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.ONFAILURE, "2.13");
        BrickStateEvent gitlab = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0");

        initialBrickStateEvents.add(jenkins);
        initialBrickStateEvents.add(nexus);
        initialBrickStateEvents.add(gitlab);

        defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        BrickStateEvent gitlabUpdated =  new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.RUNNING, "8.13.0-ce.0");;

        Set<BrickStateEvent> updateBrickStateEvents = new HashSet<>();
        updateBrickStateEvents.add(jenkins);
        updateBrickStateEvents.add(nexus);
        updateBrickStateEvents.add(gitlabUpdated);

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(updateBrickStateEvents);

        //  then

        assertThat(result).containsOnly(gitlabUpdated);
    }

    @Test
    public void update_one_brick_which_come_back_to_initial_state_repository_test() {
        //  given
        DefaultBrickStateEventRepository defaultBrickStateEventRepository = new DefaultBrickStateEventRepository();

        String projectConfigurationIdentifier = "5678";
        String stackName = "build-A";
        Set<BrickStateEvent> initialBrickStateEvents = new HashSet<>();
        BrickStateEvent jenkins = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.CI.name(), "jenkins", BrickStateEvent.State.STOPPED, "1.651.3");
        BrickStateEvent nexus = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.REPOSITORY.name(), "nexus", BrickStateEvent.State.ONFAILURE, "2.13");
        BrickStateEvent gitlab = new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0");

        initialBrickStateEvents.add(jenkins);
        initialBrickStateEvents.add(nexus);
        initialBrickStateEvents.add(gitlab);

        defaultBrickStateEventRepository.compareAndUpdate(initialBrickStateEvents);

        BrickStateEvent gitlabUpdated =  new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.RUNNING, "8.13.0-ce.0");;

        Set<BrickStateEvent> updateBrickStateEvents = new HashSet<>();
        updateBrickStateEvents.add(jenkins);
        updateBrickStateEvents.add(nexus);
        updateBrickStateEvents.add(gitlabUpdated);
        Set<BrickStateEvent> intermediareResult = defaultBrickStateEventRepository.compareAndUpdate(updateBrickStateEvents);

        BrickStateEvent gitlabUpdatedBis =  new BrickStateEvent(projectConfigurationIdentifier, stackName, BrickType.SCM.name(), "gitlab", BrickStateEvent.State.STARTING, "8.13.0-ce.0");;
        Set<BrickStateEvent> updateBrickStateEventsBis = new HashSet<>();
        updateBrickStateEventsBis.add(jenkins);
        updateBrickStateEventsBis.add(nexus);
        updateBrickStateEventsBis.add(gitlabUpdatedBis);

        //  when

        Set<BrickStateEvent> result = defaultBrickStateEventRepository.compareAndUpdate(updateBrickStateEventsBis);
        //  then

        assertThat(intermediareResult).containsOnly(gitlabUpdated);
        assertThat(result).containsOnly(gitlabUpdatedBis);
    }

}