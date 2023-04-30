package com.oberasoftware.base.event.impl;

import com.google.common.collect.ObjectArrays;
import com.oberasoftware.base.event.HandlerEntry;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
public class HandlerEntryImpl implements HandlerEntry {
    private static final Logger LOG = getLogger(HandlerEntryImpl.class);

    private final Method eventMethod;
    private final Object listenerInstance;

    public HandlerEntryImpl(Object listenerInstance, Method eventMethod) {
        this.eventMethod = eventMethod;
        this.listenerInstance = listenerInstance;
    }

    @Override
    public Optional<?> executeHandler(Object event, Object... args) {
        LOG.debug("Executing event listener: {} method: {} for event: {}", listenerInstance.getClass().getSimpleName(), eventMethod.getName(), event);
        try {
            int argumentLength = eventMethod.getParameterTypes().length;
            Object result = null;
            if(argumentLength == 1) {
                result = eventMethod.invoke(listenerInstance, event);
            } else {
                Object[] eventArguments = ObjectArrays.concat(event, args);
                if(argumentLength == eventArguments.length) {
                    LOG.debug("Calling method: {} with args: {}", eventMethod.getName(), eventArguments);
                    result = eventMethod.invoke(listenerInstance, eventArguments);
                } else {
                    LOG.debug("Cannot executed event method: {} not enough arguments: {}", event, eventArguments);
                }
            }

            if(result instanceof Optional<?>) {
                return (Optional<?>)result;
            } else {
                return Optional.ofNullable(result);
            }
        } catch (Throwable e) {
            LOG.error("Unable to execute listener instance", e);
        }

        return Optional.empty();
    }

    @Override
    public Method getEventMethod() {
        return eventMethod;
    }

    @Override
    public Object getListenerInstance() {
        return listenerInstance;
    }

    @Override
    public String toString() {
        return "HandlerEntryImpl{" +
                "eventMethod=" + eventMethod.getDeclaringClass() + "." + eventMethod.getName() +
                ", listenerInstance=" + listenerInstance +
                '}';
    }
}
