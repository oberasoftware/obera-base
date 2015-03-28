package com.oberasoftware.base.event;

/**
 * @author renarj
 */
public interface EventFilter {
    boolean isFiltered(Object event, HandlerEntry handler);
}
