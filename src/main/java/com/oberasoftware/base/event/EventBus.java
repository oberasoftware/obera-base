package com.oberasoftware.base.event;

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

    void registerHandler(EventHandler handler);

    void registerFilter(EventFilter filter);
}
