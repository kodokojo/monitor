package io.kodokojo.monitor.service;

import io.kodokojo.commons.service.actor.message.BrickStateEvent;

import java.util.Set;

public interface BrickStateLookup {


    Set<BrickStateEvent> lookup();
}
