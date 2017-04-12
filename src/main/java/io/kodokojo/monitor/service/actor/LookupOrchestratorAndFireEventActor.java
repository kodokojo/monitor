package io.kodokojo.monitor.service.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import com.google.gson.GsonBuilder;
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Receive {} brick event state, generate {} brick state change.", brickStateEvents.size(), brickStateEventsToSend.size());
            }

            if (isNotEmpty(brickStateEventsToSend)) {
                LOGGER.info("Sending {} brick state event changed.", brickStateEvents.size());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Sending following events: \n{}", new GsonBuilder().setPrettyPrinting().create().toJson(brickStateEvents));
                }
                EventBuilder builder = eventBuilderFactory.create();
                builder.setEventType(Event.BRICK_STATE_UPDATE);
                brickStateEventsToSend.forEach(brickStateEvent -> {
                    builder.addCustomHeader(Event.PROJECTCONFIGURATION_ID_CUSTOM_HEADER, brickStateEvent.getProjectConfigurationIdentifier())
                            .setPayload(brickStateEvent);
                    Event event = builder.build();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Broadcasting event :{}", Event.convertToPrettyJson(event));
                    }
                    eventBus.send(event);
                });
            }
        }).matchAny(this::unhandled)
                .build());
    }


}
