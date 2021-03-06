package io.micronaut.spring.context.aware;

import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.spring.context.MicronautApplicationContext;
import io.micronaut.spring.context.factory.MicronautBeanFactory;
import io.micronaut.spring.context.env.MicronautEnvironment;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Enables support for the interfaces {@link EnvironmentAware}, {@link ApplicationContextAware}, and {@link BeanFactoryAware}.
 *
 * @author graemerocher
 */
@Singleton
@Internal
public class SpringAwareListener implements BeanInitializedEventListener<Object>, BeanCreatedEventListener<Object> {

    private final Provider<MicronautBeanFactory> beanFactoryProvider;
    private final Provider<MicronautEnvironment> environmentProvider;
    private final Provider<MicronautApplicationContext> applicationContextProvider;

    public SpringAwareListener(Provider<MicronautBeanFactory> beanFactoryProvider, Provider<MicronautEnvironment> environmentProvider, Provider<MicronautApplicationContext> applicationContextProvider) {
        this.beanFactoryProvider = beanFactoryProvider;
        this.environmentProvider = environmentProvider;
        this.applicationContextProvider = applicationContextProvider;
    }

    @Override
    public Object onInitialized(BeanInitializingEvent<Object> event) {
        final Object bean = event.getBean();
        wireAwareObjects(bean);
        return bean;
    }

    private void wireAwareObjects(Object bean) {
        if (bean instanceof EnvironmentAware) {
            ((EnvironmentAware) bean).setEnvironment(environmentProvider.get());
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(beanFactoryProvider.get());
        }
        if (bean instanceof ApplicationContextAware) {
            ((ApplicationContextAware) bean).setApplicationContext(applicationContextProvider.get());
        }
    }

    @Override
    public Object onCreated(BeanCreatedEvent<Object> event) {
        final Object bean = event.getBean();
        return onBeanCreated(bean);
    }

    public Object onBeanCreated(Object bean) {
        wireAwareObjects(bean);
        if (bean instanceof InitializingBean) {
            try {
                ((InitializingBean) bean).afterPropertiesSet();
            } catch (Exception e) {
                throw new BeanCreationException(e.getMessage(), e);
            }
        }
        return bean;
    }
}
