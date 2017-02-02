package io.kodokojo.monitor.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.kodokojo.commons.config.MarathonConfig;
import io.kodokojo.commons.config.module.OrchestratorConfig;
import io.kodokojo.commons.config.properties.PropertyConfig;
import io.kodokojo.commons.config.properties.PropertyResolver;
import io.kodokojo.commons.config.properties.provider.PropertyValueProvider;

public class PropertyModule extends AbstractModule {
    @Override
    protected void configure() {
        //
    }

    @Provides
    @Singleton
    MarathonConfig provideMarathonConfig(PropertyValueProvider valueProvider) {
        return createConfig(MarathonConfig.class, valueProvider);
    }

    @Provides
    @Singleton
    OrchestratorConfig provideOrchestratorConfig(PropertyValueProvider valueProvider) {
        return createConfig(OrchestratorConfig.class, valueProvider);
    }


    private <T extends PropertyConfig> T createConfig(Class<T> configClass, PropertyValueProvider valueProvider) {
        PropertyResolver resolver = new PropertyResolver(valueProvider);
        return resolver.createProxy(configClass);
    }

}
