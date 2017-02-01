package io.kodokojo.monitor.service;

import io.kodokojo.commons.service.actor.message.BrickStateEvent;

import java.util.Set;

public interface BrickStateEventRepository {

    Set<BrickStateEvent> compareAndUpdate(Set<BrickStateEvent> brickStateEvents);

}
