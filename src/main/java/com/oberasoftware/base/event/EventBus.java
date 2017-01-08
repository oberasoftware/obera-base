package com.oberasoftware.base.event;

import java.util.concurrent.TimeUnit;

/**
 * @author renarj
 */
public interface EventBus {
    /**
     * Publish an event onto the event bus, the event could be handled asynchronously.
     * @param event The event to publish
     * @param arguments optional arguments that can be passed to the method of the event handler, will be ignored if event handler cannot accept these
     */
    void publish(Event event, Object... arguments);

    /**
     * Publish a synchronous event onto the event bus, the event could be handled asynchronously.
     * @param event The event to publish
     * @param unit The unit of the waiting time
     * @param time The time to wait for the event to complete
     * @param arguments optional arguments that can be passed to the method of the event handler, will be ignored if event handler cannot accept these
     * @return True if completed, False if not
     */
    boolean publishSync(Event event, TimeUnit unit, long time, Object... arguments);

    void registerHandler(EventHandler handler);

    void registerFilter(EventFilter filter);
}
