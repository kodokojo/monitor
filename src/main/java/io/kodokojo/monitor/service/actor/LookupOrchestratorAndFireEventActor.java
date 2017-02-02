package io.kodokojo.monitor.service.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import io.kodokojo.commons.event.Event;
import io.kodokojo.commons.event.EventBuilder;
import io.kodokojo.commons.event.EventBuilderFactory;
import io.kodokojo.commons.event.EventBus;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.monitor.service.BrickStateEventRepository;
import io.kodokojo.monitor.service.BrickStateLookup;

import java.util.Set;

import static akka.event.Logging.getLogger;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

public class LookupOrchestratorAndFireEventActor extends AbstractActor {

    private final LoggingAdapter LOGGER = getLogger(getContext().system(), this);

    public static Props PROPS(BrickStateLookup brickStateLookup, BrickStateEventRepository brickStateEventRepository, EventBuilderFactory eventBuilderFactory, EventBus eventBus) {
        requireNonNull(brickStateLookup, "brickStateLookup must be defined.");
        requireNonNull(brickStateEventRepository, "brickStateEventRepository must be defined.");
        requireNonNull(eventBus, "eventBus must be defined.");
        return Props.create(LookupOrchestratorAndFireEventActor.class, brickStateLookup, brickStateEventRepository, eventBuilderFactory, eventBus);
    }

    public LookupOrchestratorAndFireEventActor(BrickStateLookup brickStateLookup, BrickStateEventRepository brickStateEventRepository, EventBuilderFactory eventBuilderFactory, EventBus eventBus) {

        receive(ReceiveBuilder.match(String.class, msg -> {
            LOGGER.debug("Receive a tick.");
            Set<BrickStateEvent> brickStateEvents = brickStateLookup.lookup();
            Set<BrickStateEvent> brickStateEventsToSend = brickStateEventRepository.compareAndUpdate(brickStateEvents);

            if (isNotEmpty(brickStateEvents)) {
                LOGGER.info("Sending {} brick state event changed.", brickStateEvents.size());
                EventBuilder builder = eventBuilderFactory.create();
                brickStateEventsToSend.forEach(brickStateEvent -> {

                    builder.setEventType(Event.BRICK_STATE_UPDATE)
                            .setPayload(brickStateEvent);
                    eventBus.send(builder.build());

                });
            }
        }).matchAny(this::unhandled)
                .build());
    }


}
