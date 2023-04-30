package com.oberasoftware.base.event;

import com.oberasoftware.base.event.impl.LocalEventBus;
import org.junit.Test;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author renarj
 */
public class EventBusTest {
    private static final Logger LOG = getLogger(EventBusTest.class);

    private static final int TEST_ID = 15;
    private static final int TEST_VALUE = 105;

    @Test
    public void testEventReceive() {
        EventBus bus = new LocalEventBus();

        final CountDownLatch latch = new CountDownLatch(1);
        TestEventHandler handler = new TestEventHandler(latch);
        bus.registerHandler(handler);

        bus.publish(new TestEvent(TEST_ID));
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);

        assertThat(handler.getId(), is(TEST_ID));
        assertThat(handler.getValue(), is(-1));
    }

    @Test
    public void testEventFilter() {
        EventBus bus = new LocalEventBus();

        final CountDownLatch latch = new CountDownLatch(1);
        TestEventHandler handler = new TestEventHandler(latch);
        bus.registerHandler(handler);
        bus.registerFilter((event, handler1) -> {
            TestAnnotation annotation = handler1.getEventMethod().getAnnotation(TestAnnotation.class);
            if (annotation != null) {
                LOG.debug("Minimum ID is: {}", annotation.minId());
                return event instanceof TestEvent && ((TestEvent) event).id() < annotation.minId();
            }
            return false;
        });

        bus.publish(new TestEvent(1));
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);

        assertThat(handler.getId(), is(-1));


        bus.publish(new TestEvent(TEST_ID));
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);
        assertThat(handler.getId(), is(TEST_ID));
    }

    @Test
    public void testEventExtraParameters() {
        EventBus bus = new LocalEventBus();

        final CountDownLatch latch = new CountDownLatch(2);
        TestEventHandler handler = new TestEventHandler(latch);
        bus.registerHandler(handler);

        bus.publish(new TestEvent(TEST_ID), TEST_VALUE);
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);

        assertThat(handler.getId(), is(TEST_ID));
        assertThat(handler.getValue(), is(TEST_VALUE));
    }

    @Test
    public void testProduceEvent() {
        EventBus bus = new LocalEventBus();

        final CountDownLatch latch = new CountDownLatch(1);
        TestEventHandler handler = new TestEventHandler(latch);
        bus.registerHandler(handler);

        bus.publish(new AnotherEvent(TEST_ID));
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);

        assertThat(handler.getId(), is(TEST_ID));
        assertThat(handler.getCounter(), is(1));
    }

    @Test
    public void testProducesCollection() {
        EventBus bus = new LocalEventBus();

        final CountDownLatch latch = new CountDownLatch(11);
        TestEventHandler handler = new TestEventHandler(latch);
        bus.registerHandler(handler);

        bus.publish(new AnotherEvent(TEST_ID), 10);
        awaitUninterruptibly(latch, 2, TimeUnit.SECONDS);



        assertThat(handler.getCounter(), is(11));
    }

    public static class TestEventHandler implements EventHandler {

        private final CountDownLatch latch;
        private volatile int id = -1;
        private volatile int value = -1;
        private final AtomicInteger counter = new AtomicInteger(0);

        public TestEventHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getId() {
            return id;
        }

        public int getValue() {
            return value;
        }

        public int getCounter() {
            return counter.get();
        }

        @EventSubscribe
        @TestAnnotation(minId = 10)
        public void handle(TestEvent event) {
            LOG.debug("Received event with id: {}", event.id());
            this.id = event.id();
            this.counter.incrementAndGet();
            latch.countDown();
        }

        @EventSubscribe
        public void handleWithMoreParameters(TestEvent event, int value) {
            LOG.debug("Received event: {} with additional parameter: {}", event, value);
            this.value = value;
            latch.countDown();
        }

        @EventSubscribe
        public Optional<Event> producesEvent(AnotherEvent event) {
            LOG.debug("Received another event: {} creating test event", event);
            return Optional.of(new TestEvent(event.id()));
        }

        @EventSubscribe
        public List<Event> produceCollectionEvents(AnotherEvent event, int amount) {
            LOG.debug("Received another event: {} creating collection of test events with amount: {}", event, amount);
            List<Event> events = new ArrayList<>();
            for(int i=0; i<amount; i++) {
                events.add(new TestEvent(event.id() + i));
            }
            return events;
        }
    }

    public record TestEvent(int id) implements Event {
        @Override
        public String toString() {
            return "TestEvent{" +
                    "id=" + id +
                    '}';
        }
    }

    public record AnotherEvent(int id) implements Event {

        @Override
            public String toString() {
                return "AnotherEvent{" +
                        "id=" + id +
                        '}';
            }
        }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface TestAnnotation {
        int minId();
    }
}
