package com.oberasoftware.base.event;

/**
 * This is a distributed event bus interface, can be used for event busses like JMS, Kafka, etc.
 *
 * @author Renze de Vries
 */
public interface DistributedEventBus extends EventBus {
    /**
     * Connects to a distributed event bus / cluster
     */
    void connect();

    /**
     * Disconnects from the distributed event bus / cluster
     */
    void disconnect();
}
