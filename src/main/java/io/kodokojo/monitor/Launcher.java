/**
 * Kodo Kojo - API frontend which dispatch REST event to Http services or publish event on EvetnBus.
 * Copyright © 2016 Kodo Kojo (infos@kodokojo.io)
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.kodokojo.monitor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.google.inject.*;
import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.config.MicroServiceConfig;
import io.kodokojo.commons.config.module.*;
import io.kodokojo.commons.event.EventBus;
import io.kodokojo.commons.model.BrickType;
import io.kodokojo.commons.service.BrickFactory;
import io.kodokojo.commons.service.actor.message.BrickStateEvent;
import io.kodokojo.commons.service.lifecycle.ApplicationLifeCycleManager;
import io.kodokojo.commons.service.repository.ProjectFetcher;
import io.kodokojo.monitor.config.module.PropertyModule;
import io.kodokojo.monitor.config.module.ServiceModule;
import io.kodokojo.monitor.service.BrickStateEventRepository;
import io.kodokojo.monitor.service.BrickStateLookup;
import io.kodokojo.monitor.service.marathon.MarathonBrickStateLookup;
import okhttp3.OkHttpClient;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Launcher {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    private static final String MOCK = "mock";

    public static void main(String[] args) {


        Injector propertyInjector = Guice.createInjector(new CommonsPropertyModule(args), new PropertyModule());
        MicroServiceConfig microServiceConfig = propertyInjector.getInstance(MicroServiceConfig.class);
        LOGGER.info("Starting Kodo Kojo {}.", microServiceConfig.name());
        Injector commonsServicesInjector = propertyInjector.createChildInjector(new UtilityServiceModule(), new EventBusModule(), new DatabaseModule(), new SecurityModule());

        Injector marathonInjector = null;
        OrchestratorConfig orchestratorConfig = propertyInjector.getInstance(OrchestratorConfig.class);
        if (MOCK.equals(orchestratorConfig.orchestrator())) {
            marathonInjector = commonsServicesInjector.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    //
                }

                @Singleton
                @Provides
                BrickStateLookup provideBrickStateLookup() {
                    return new BrickStateLookup() {

                        private int cpt = 0;

                        @Override
                        public Set<BrickStateEvent> lookup() {
                            return Collections.singleton(generateBrickStateEvent());
                        }

                        public BrickStateEvent generateBrickStateEvent() {
                            BrickStateEvent.State[] states = BrickStateEvent.State.values();
                            int index = RandomUtils.nextInt(states.length);
                            BrickStateEvent.State state = states[index];

                            return new BrickStateEvent(
                                    "1234",
                                    "build-A",
                                    BrickType.CI.name(),
                                    "jenkins",
                                    state,
                                    "1.65Z3.1"
                            );
                        }
                    };
                }
            });
        } else {
            marathonInjector = commonsServicesInjector.createChildInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    //
                }

                @Singleton
                @Provides
                BrickStateLookup provideBrickStateLookup(MarathonConfig marathonConfig, ProjectFetcher projectFectcher, BrickFactory brickFactory, OkHttpClient httpClient) {
                    return new MarathonBrickStateLookup(marathonConfig, projectFectcher, brickFactory, httpClient);
                }

            });
        }

        Injector servicesInjector = marathonInjector.createChildInjector(new ServiceModule());

        ApplicationLifeCycleManager applicationLifeCycleManager = servicesInjector.getInstance(ApplicationLifeCycleManager.class);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                LOGGER.info("Stopping services.");
                applicationLifeCycleManager.stop();
                LOGGER.info("All services stopped.");
            }
        });

        EventBus eventBus = servicesInjector.getInstance(EventBus.class);
        eventBus.connect();
        //  Init repository.
        BrickStateLookup brickStateLookup = servicesInjector.getInstance(BrickStateLookup.class);
        BrickStateEventRepository repository = servicesInjector.getInstance(BrickStateEventRepository.class);
        Set<BrickStateEvent> brickStateEvents = brickStateLookup.lookup();
        repository.compareAndUpdate(brickStateEvents);


        ActorSystem actorSystem = servicesInjector.getInstance(ActorSystem.class);
        ActorRef actorRef = servicesInjector.getInstance(ActorRef.class);
        actorSystem.scheduler().schedule(Duration.Zero(), Duration.create(1, TimeUnit.MINUTES), actorRef, "Tick", actorSystem.dispatcher(), ActorRef.noSender());
        LOGGER.info("Kodo Kojo {} started.", microServiceConfig.name());

    }

}
