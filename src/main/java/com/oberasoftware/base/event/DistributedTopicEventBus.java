package com.oberasoftware.base.event;

import java.util.List;

/**
 * This is a distirbuted topic driven event bus
 *
 * @author Renze de Vries
 */
public interface DistributedTopicEventBus extends DistributedEventBus {
    /**
     * Subscribes to a topic on the distributed event bus
     * @param topic The topic
     */
    void subscribe(String topic);

    /**
     * Gets the active subscriptions for this event bus connector
     * @return The list of active subscriptions, empty list if none
     */
    List<String> getSubscriptions();

    /**
     * Unsubscribes from a given topic
     * @param topic The topic
     */
    void unsubscribe(String topic);
}
