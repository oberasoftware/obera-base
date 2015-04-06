package com.oberasoftware.base.event;

import com.oberasoftware.base.event.impl.LocalEventBus;
import org.junit.Test;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
            LOG.debug("Minimum ID is: {}", annotation.minId());
            return event instanceof TestEvent && ((TestEvent) event).getId() < annotation.minId();
        });

        bus.publish(new TestEvent(1));
        awaitUninterruptibly(latch, 1, TimeUnit.SECONDS);

        assertThat(handler.getId(), is(-1));


        bus.publish(new TestEvent(TEST_ID));
        awaitUninterruptibly(latch, 5, TimeUnit.SECONDS);
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

    public class TestEventHandler implements EventHandler {

        private final CountDownLatch latch;
        private AtomicInteger id = new AtomicInteger(-1);
        private AtomicInteger value = new AtomicInteger(-1);

        public TestEventHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        public int getId() {
            return id.get();
        }

        public int getValue() {
            return value.get();
        }

        @EventSubscribe
        @TestAnnotation(minId = 10)
        public void handle(TestEvent event) {
            LOG.debug("Received event with id: {}", event.getId());
            try {
                this.id.set(event.getId());
            } finally {
                latch.countDown();
            }
        }

        @EventSubscribe
        public void handleWithMoreParameters(TestEvent event, int value) {
            LOG.debug("Received event: {} with additional parameter: {}", event, value);
            try {
                this.value.set(value);
            } finally {
                latch.countDown();
            }
        }
    }

    public class TestEvent implements Event {
        private final int id;

        private TestEvent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "TestEvent{" +
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
