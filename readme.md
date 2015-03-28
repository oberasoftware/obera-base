This repository contains base libraries that are used in the development of Oberasoftware.com products.

##EventBus
There is a basic event bus based publisher mechanism that can be used for asynchronous event processing on multiple 
threads using easy mechanisms to receive and register event handlers.

### Spring and EventBus
If you want to use the EventBus in your spring application its a simple matter of importing the following Spring configuration Bean: 
```java
com.oberasoftware.base.BaseConfiguration
```

In your application you can then simple inject the following bean:
```java
com.oberasoftware.base.event.EventBus
```

All event listeners and event filter classes will be automatically detected if they are present in the spring context. You need to make sure that your event listeners use the following
marker interface 
```java
com.oberasoftware.base.event.EventHandler
```

In case you have implemented an EventFilter the following marker interface needs to be used:
```java
com.oberasoftware.base.event.EventFilter
```

### Create Local EventBus
In case you want to create an event bus in your code without using Spring the following code can be used:
```java
EventBus bus = new LocalEventBus();
bus.registerHandler(new EventHandler() {
    @EventSubscribe
    public void receiveMyEvent(MyEvent event) {                    
    }
});
```

### Registering EventHandlers
In order to register any event handler you can simply register new handlers on the event bus using the ```registerHandler``` method. The class provided in the registerHandler 
 will be scanned for methods that are capable of receiving events. In order to indicate a method is capable of handling events it needs to be annotated with the ```@EventSubscribe``` annotation and then 
 will be used for event handling.
 
#### Event handler typing
By default any eventhandler will receive all events that it is capable of receiving. This means if your EventHandler can receive a java.lang.Object it will receive every possible
event that was posted to the EventBus. You can simply reduce the scope by making the receiving Event of a lower type for example if you are only interested in your specific event simply
have the first argument in the event handler method be of that specific type. See below examples of a generic vs specific event handler. The framework will handle all simply typing for 
you and as a event handler you dont have to worry about casting at all.

Generic Receives any event:
```java
bus.registerHandler(new EventHandler() {
    @EventSubscribe
    public void receiveMyEvent(Event event) {                    
    }
});
```

Specific event handler:
```java
bus.registerHandler(new EventHandler() {
    @EventSubscribe
    public void receiveMyEvent(MySpecificEvent event) {                    
    }
});
```

 
### Registering EventFilters
Sometimes you want to add some standard decision logic when an event handler needs to handle an incoming event. By default any EventHandler will receive 
all the types it is suitable to receive. The EventFilter is a pre-receive hook that allows filtering of incoming events for specific EventHandlers.

In the following example we filter the events received by the handler by testing if a minimum configured value has 
reached. The configuration is read from an annotation on the handler method.
```java
bus.registerFilter((event, handler1) -> {
    TestAnnotation annotation = handler1.getEventMethod().getAnnotation(TestAnnotation.class);
    LOG.debug("Minimum ID is: {}", annotation.minId());
    return event instanceof TestEvent && ((TestEvent) event).getId() < annotation.minId();
});
```