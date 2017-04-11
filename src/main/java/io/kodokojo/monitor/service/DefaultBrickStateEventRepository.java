package io.kodokojo.monitor.service;

import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DefaultBrickStateEventRepository implements BrickStateEventRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBrickStateEventRepository.class);

    private Set<BrickStateEvent> cache = new HashSet<>();

    @Override
    public Set<BrickStateEvent> compareAndUpdate(Set<BrickStateEvent> brickStateEvents) {
        requireNonNull(brickStateEvents, "brickStateEvents must be defined.");
        Set<BrickStateEvent> res = new HashSet<>();

        // Add update a brick state event
        final Map<BrickStateEvent, BrickStateEvent> toUpdate = new HashMap<>();

        Set<BrickStateEvent> toAdd = brickStateEvents.stream()
                .filter(brickStateEvent -> mustBeAddOrUpdate(brickStateEvent, toUpdate))
                .collect(Collectors.toSet());
        toUpdate.entrySet().forEach(entry -> {
            cache.remove(entry.getKey());
            cache.add(entry.getValue());
            res.add(entry.getValue());
        });
        cache.addAll(toAdd);
        res.addAll(toAdd);

        //  Remove not anymore existing brick from Marathon
        Set<BrickStateEvent> toRemove = cache.stream()
                .filter(cachedBrickStateEvent -> !containsBrickStateEvent(brickStateEvents, cachedBrickStateEvent).isPresent())
                .collect(Collectors.toSet());
        cache.removeAll(toRemove);
        toRemove.forEach(b -> {
            BrickStateEvent.State state = BrickStateEvent.State.STOPPED;
            BrickStateEvent unknownBrickStateEvent = new BrickStateEvent(
                    b.getProjectConfigurationIdentifier(),
                    b.getStackName(),
                    b.getBrickType(),
                    b.getBrickName(),
                    state,
                    b.getVersion()
            );
            LOGGER.info("Remove brick {} from projectConfigurationId '{}'; Brick not anymore defined in Marathon. Generating BrickStateEvent {}", b.getBrickName(), b.getProjectConfigurationIdentifier(), state);
            res.add(unknownBrickStateEvent);
        });

        return res;
    }

    /**
     * Check if brickStateEvent exit in cache, if must be add, return <code>true</code>. If need to be update, element is add to
     * <code>toUpdate</code> Set. if must be ignore, return <code>false</code>.
     * @param brickStateEvent element to challenge in cache.
     * @param toUpdate add all element which may be update
     * @return return true to add, false to ignore.
     */
    private boolean mustBeAddOrUpdate(BrickStateEvent brickStateEvent, Map<BrickStateEvent, BrickStateEvent> toUpdate) {
        Optional<BrickStateEvent> containsBrickStateEvent = containsBrickStateEventInCache(brickStateEvent);
        if (containsBrickStateEvent.isPresent())  {
            BrickStateEvent cachedBrickStateEvent = containsBrickStateEvent.get();
            if (brickStateEventMustBeUpdate(cachedBrickStateEvent, brickStateEvent)) {
                toUpdate.put(cachedBrickStateEvent, brickStateEvent);
            }
            return false;
        }
        return true;
    }

    private Optional<BrickStateEvent> containsBrickStateEventInCache(BrickStateEvent brickStateEvent) {
        return containsBrickStateEvent(cache, brickStateEvent);
    }

    private Optional<BrickStateEvent> containsBrickStateEvent(Set<BrickStateEvent> collection, BrickStateEvent brickStateEvent) {
        return collection.stream().filter(cachedBrickStateEvent -> matchBrickStateEvent(cachedBrickStateEvent, brickStateEvent)).findFirst();
    }

    private static boolean brickStateEventMustBeUpdate(BrickStateEvent a, BrickStateEvent b) {
        return matchBrickStateEvent(a, b) &&
                a.getState() != b.getState();
    }

    private static boolean matchBrickStateEvent(BrickStateEvent a, BrickStateEvent b) {
        return a.getProjectConfigurationIdentifier().equals(b.getProjectConfigurationIdentifier()) &&
                a.getStackName().equals(b.getStackName()) &&
                a.getBrickName().equals(b.getBrickName());
    }

}
