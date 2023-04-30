package com.oberasoftware.base.event.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.oberasoftware.base.event.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

import static java.util.Arrays.stream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
@Component
public class LocalEventBus implements EventBus {
    private static final Logger LOG = getLogger(LocalEventBus.class);

    private final Map<Class<?>, List<HandlerEntryImpl>> handlerEntries = new ConcurrentHashMap<>();

    private final List<EventFilter> activeFilters = Lists.newCopyOnWriteArrayList();

    @Autowired(required = false)
    private List<EventHandler> eventHandlers;

    @Autowired(required = false)
    private List<EventFilter> eventFilters;

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

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
        execute(event, args);
    }

    @Override
    public boolean publishSync(Event event, TimeUnit unit, long time, Object... args) {
        Future<?> future = execute(event, args);
        try {
            future.get(time, unit);
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Interrupted waiting for event response: {}", e.getMessage());
            return false;
        }
    }

    private Future<?> execute(Event event, Object... args) {
        return executorService.submit(() -> {
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
        LOG.trace("Executing handler: {} for event: {}", h, event);
        String handlerClass = h.getListenerInstance().getClass().getName();
        String handlerId = handlerClass + "." + h.getEventMethod().getName();
        if(!isFiltered(h, event)) {
            LOG.trace("Event is not filtered: {}", event);
            if (!handlersExecuted.contains(handlerId)) {

                handleResult(h.executeHandler(event, args));
            }
        } else {
            LOG.trace("Event: {} is filtered", event);
        }
        return handlerId;
    }

    private void handleResult(Optional<?> optionalResult) {
        if (optionalResult.isPresent()) {
            Object result = optionalResult.get();

            if(result instanceof Collection<?>) {
                ((Collection<?>)result).forEach(this::publishResult);
            } else {
                publishResult(result);
            }
        }
    }

    private void publishResult(Object result) {
        if(result instanceof Event) {
            LOG.debug("Handler produced a result of type Event, sending to bus: {}", result);
            publish((Event) result);
        }
    }

    private boolean isFiltered(HandlerEntry handlerEntry, Object event) {
        return activeFilters.stream().anyMatch(f -> {
            LOG.trace("Checking filters for event: {}", event);
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
