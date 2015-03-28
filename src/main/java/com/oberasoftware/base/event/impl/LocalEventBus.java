package com.oberasoftware.base.event.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.oberasoftware.base.event.Event;
import com.oberasoftware.base.event.EventBus;
import com.oberasoftware.base.event.EventFilter;
import com.oberasoftware.base.event.EventHandler;
import com.oberasoftware.base.event.EventSubscribe;
import com.oberasoftware.base.event.HandlerEntry;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.stream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class LocalEventBus implements EventBus {
    private static final Logger LOG = getLogger(LocalEventBus.class);

    private Map<Class<?>, List<HandlerEntryImpl>> handlerEntries = new ConcurrentHashMap<>();

    private List<EventFilter> activeFilters = Lists.newCopyOnWriteArrayList();

    @Autowired(required = false)
    private List<EventHandler> eventHandlers;

    @Autowired(required = false)
    private List<EventFilter> eventFilters;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @PostConstruct
    public void loadListeners() {
        if(eventHandlers != null) {
            LOG.debug("Adding all event listeners: {}", eventHandlers.size());
            eventHandlers.forEach(this::processEventListener);
        }

        if(eventFilters != null) {
            LOG.debug("Adding detected event filters: {}", eventFilters.size());
            eventFilters.forEach(this::registerFilter);
        }
    }

    @Override
    public void publish(Event event, Object... args) {
        executorService.submit(() -> {
            LOG.debug("Firing off an Async event: {}", event);
            notifyEventListeners(event, args);
        });
    }

    @Override
    public void registerHandler(EventHandler handler) {
        LOG.debug("Registering handler: {}", handler);
        processEventListener(handler);
    }

    @Override
    public void registerFilter(EventFilter filter) {
        LOG.debug("Registering filter: {}", filter);
        activeFilters.add(filter);
    }

    private void notifyEventListeners(Object event, Object... args) {
        Set<String> handlersExecuted = new HashSet<>();
        TypeToken.of(event.getClass()).getTypes().forEach(o -> {
            List<HandlerEntryImpl> handlers = handlerEntries.getOrDefault(o.getRawType(), new ArrayList<>());
            handlers.forEach(h -> {
                String handlerId = executeHandler(h, event, handlersExecuted, args);
                handlersExecuted.add(handlerId);
            });
        });
    }

    private String executeHandler(HandlerEntry h, Object event, Set<String> handlersExecuted, Object... args) {
        LOG.debug("Executing handler: {} for event: {}", h, event);
        String handlerClass = h.getListenerInstance().getClass().getName();
        String handlerId = handlerClass + "." + h.getEventMethod().getName();
        if(!isFiltered(h, event)) {
            LOG.debug("Event is not filtered: {}", event);
            if (!handlersExecuted.contains(handlerId)) {

                Optional<?> result = h.executeHandler(event, args);
                if (result.isPresent() && result.get() instanceof Event) {
                    LOG.debug("Handler produced a result of type Event, sending to bus: {}", result.get());
                    publish((Event) result.get());
                }
            }
        } else {
            LOG.debug("Event: {} is filtered", event);
        }
        return handlerId;
    }

    private boolean isFiltered(HandlerEntry handlerEntry, Object event) {
        return activeFilters.stream().anyMatch(f -> {
            LOG.debug("Filtering event: {}", event);
            return f.isFiltered(event, handlerEntry);
        });
    }

    private void processEventListener(Object listenerInstance) {
        LOG.debug("Processing event listener: {}", listenerInstance);
        Class<?> eventListenerClass = listenerInstance.getClass();
        stream(eventListenerClass.getMethods())
                .filter(m -> m.getDeclaredAnnotation(EventSubscribe.class) != null)
                .filter(m -> !m.isBridge())
                .forEach(m -> addEventHandler(listenerInstance, m));
    }

    private void addEventHandler(Object listenerInstance, Method method) {
        LOG.debug("Loading parameter type on method: {}", method.getName());
        Class<?>[] parameterTypes = method.getParameterTypes();
        if(parameterTypes.length > 0) {
            LOG.debug("Interested in message type: {}", parameterTypes[0].getSimpleName());
            //only checking first type
            Class<?> parameterType = parameterTypes[0];

            handlerEntries.computeIfAbsent(parameterType, v -> new ArrayList<>());
            handlerEntries.get(parameterType).add(new HandlerEntryImpl(listenerInstance, method));
        }
    }
}
