package io.kodokojo.monitor.service.actor;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestActorRef;
import io.kodokojo.commons.config.MicroServiceConfig;
import io.kodokojo.commons.event.DefaultEventBuilderFactory;
import io.kodokojo.commons.event.Event;
import io.kodokojo.commons.event.EventBuilderFactory;
import io.kodokojo.commons.event.EventBus;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.monitor.service.BrickStateEventRepository;
import io.kodokojo.monitor.service.BrickStateLookup;
import io.kodokojo.monitor.service.MonitorDataBuilder;
import io.kodokojo.test.DataBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LookupOrchestratorAndFireEventActorTest implements DataBuilder, MonitorDataBuilder{

    private ActorSystem actorSystem;

    private BrickStateLookup brickStateLookup;

    private BrickStateEventRepository brickStateEventRepository;

    private EventBus eventBus;

    private EventBuilderFactory eventBuilderFactory;


    @Before
    public void setup() {
        actorSystem = ActorSystem.create();
        brickStateLookup = mock(BrickStateLookup.class);
        brickStateEventRepository = mock(BrickStateEventRepository.class);
        eventBus = mock(EventBus.class);
        String uuid = UUID.randomUUID().toString();
        eventBuilderFactory = new DefaultEventBuilderFactory(new MicroServiceConfig() {
            @Override
            public String name() {
                return "mock";
            }

            @Override
            public String uuid() {
                return uuid;
            }
        });
    }

    @Test
    public void send_one_initial_brick_state_update() {
        // given

        BrickStateEvent brickStateEvent = aBrickStateEvents().iterator().next();
            Set<BrickStateEvent> brickStateEvents = Collections.singleton(brickStateEvent);
        when(brickStateLookup.lookup()).thenAnswer(invocationOnMock -> brickStateEvents);

        when(brickStateEventRepository.compareAndUpdate(brickStateEvents)).thenReturn(brickStateEvents);

        TestActorRef<Actor> subject = TestActorRef.create(
                actorSystem,
                LookupOrchestratorAndFireEventActor.PROPS(
                        brickStateLookup,
                        brickStateEventRepository,
                        eventBuilderFactory,
                        eventBus
                ));

        // when

        subject.tell("Coucou", ActorRef.noSender());

        // then

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

        verify(eventBus).send(captor.capture());

        Event eventSent = captor.getValue();
        assertThat(eventSent).isNotNull();
        assertThat(eventSent.getEventType()).isEqualTo(Event.BRICK_STATE_UPDATE);
        BrickStateEvent payload = eventSent.getPayload(BrickStateEvent.class);
        assertThat(payload).isNotNull();
        assertThat(payload.getState()).isEqualTo(brickStateEvent.getState());
        assertThat(payload.getProjectConfigurationIdentifier()).isEqualTo(brickStateEvent.getProjectConfigurationIdentifier());
        assertThat(payload.getBrickName()).isEqualTo(brickStateEvent.getBrickName());
    }

}