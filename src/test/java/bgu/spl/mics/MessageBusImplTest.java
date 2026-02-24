package bgu.spl.mics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MessageBusImplTest {

    private MessageBusImpl messageBus;
    private MicroService testMicroService;

    @BeforeEach
    public void setUp() {
        messageBus = MessageBusImpl.getInstance();
        testMicroService = new MicroService("TestService") {
            @Override
            protected void initialize() {
                // Empty initialization for testing
            }
        };
        messageBus.register(testMicroService);
    }

    @Test
    public void testSubscribeEvent() {
        messageBus.subscribeEvent(TestEvent.class, testMicroService);

        Future<String> future = messageBus.sendEvent(new TestEvent());
        assertNotNull(future, "Event should be sent and future returned.");
    }

    @Test
    public void testSubscribeBroadcast() {
        messageBus.subscribeBroadcast(TestBroadcast.class, testMicroService);

        messageBus.sendBroadcast(new TestBroadcast());
        try {
            Message message = messageBus.awaitMessage(testMicroService);
            assertTrue(message instanceof TestBroadcast, "MicroService should receive the broadcast message.");
        } catch (InterruptedException e) {
            fail("awaitMessage should not throw an exception.");
        }
    }
    
    @Test
    public void testSendBroadcast() {
        messageBus.subscribeBroadcast(TestBroadcast.class, testMicroService);
        TestBroadcast broadcast = new TestBroadcast();
        messageBus.sendBroadcast(broadcast);

        try {
            Message message = messageBus.awaitMessage(testMicroService);
            assertEquals(broadcast, message, "Broadcast should be received by the MicroService.");
        } catch (InterruptedException e) {
            fail("awaitMessage should not throw an exception.");
        }
    }

    @Test
    public void testAwaitMessage() {
        TestBroadcast broadcast = new TestBroadcast();
        messageBus.subscribeBroadcast(TestBroadcast.class, testMicroService);
        messageBus.sendBroadcast(broadcast);

        try {
            Message message = messageBus.awaitMessage(testMicroService);
            assertEquals(broadcast, message, "awaitMessage should return the correct message.");
        } catch (InterruptedException e) {
            fail("awaitMessage should not throw an exception.");
        }
    }
    private static class TestEvent implements Event<String> {
        // Event implementation for testing
    }

    private static class TestBroadcast implements Broadcast {
        // Broadcast implementation for testing
    }
}
