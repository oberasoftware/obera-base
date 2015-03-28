package com.oberasoftware.base.event;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author renarj
 */
public interface HandlerEntry {
    Optional<?> executeHandler(Object event, Object... args);

    Method getEventMethod();

    Object getListenerInstance();
}
