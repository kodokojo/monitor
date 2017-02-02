package io.kodokojo.monitor.config.module;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.config.module.OrchestratorConfig;
import io.kodokojo.commons.config.properties.PropertyConfig;
import io.kodokojo.commons.config.properties.PropertyResolver;
import io.kodokojo.commons.config.properties.provider.PropertyValueProvider;
import io.kodokojo.commons.event.EventBuilderFactory;
import io.kodokojo.commons.event.EventBus;
import io.kodokojo.commons.service.BrickFactory;
import io.kodokojo.commons.service.actor.DeadLetterActor;
import io.kodokojo.commons.service.repository.ProjectFetcher;
import io.kodokojo.monitor.service.BrickStateEventRepository;
import io.kodokojo.monitor.service.BrickStateLookup;
import io.kodokojo.monitor.service.DefaultBrickStateEventRepository;
import io.kodokojo.monitor.service.actor.LookupOrchestratorAndFireEventActor;
import io.kodokojo.monitor.service.marathon.MarathonBrickStateLookup;
import okhttp3.OkHttpClient;

public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        ActorSystem actorSystem = ActorSystem.apply("kodokojo");
        ActorRef deadletterlistener = actorSystem.actorOf(DeadLetterActor.PROPS(), "deadletterlistener");
        actorSystem.eventStream().subscribe(deadletterlistener, DeadLetter.class);
        bind(ActorSystem.class).toInstance(actorSystem);
    }

    @Provides
    @Singleton
    ActorRef provideActor(ActorSystem actorSystem, BrickStateLookup brickStateLookup, BrickStateEventRepository brickStateEventRepository, EventBuilderFactory eventBuilderFactory, EventBus eventBus) {
        return actorSystem.actorOf(LookupOrchestratorAndFireEventActor.PROPS(brickStateLookup, brickStateEventRepository, eventBuilderFactory, eventBus));
    }

    @Provides
    @Singleton
    BrickStateEventRepository provideBrickStateEventRepository() {
        return new DefaultBrickStateEventRepository();
    }


}
