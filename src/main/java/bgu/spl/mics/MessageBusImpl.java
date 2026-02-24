package bgu.spl.mics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The {@link MessageBusImpl} class is the implementation of the MessageBus interface.
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class.
 * All other methods and members you add to the class must be private.
 */
public class MessageBusImpl implements MessageBus {
    private final ConcurrentHashMap<MicroService, BlockingQueue<Message>> microserviceQueues;
    private final ConcurrentHashMap<Class<? extends Message>, LinkedBlockingQueue<MicroService>> subscriptions;
    private final ConcurrentHashMap<Event<?>, Future<?>> eventFutures;

    private static class MessageBusHolder {
        private static final MessageBusImpl INSTANCE = new MessageBusImpl();
    }

    private MessageBusImpl() {
        microserviceQueues = new ConcurrentHashMap<>();
        subscriptions = new ConcurrentHashMap<>();
        eventFutures = new ConcurrentHashMap<>();
    }

    public static MessageBusImpl getInstance() {
        return MessageBusHolder.INSTANCE;
    }

    @Override
    public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
        subscriptions.putIfAbsent(type, new LinkedBlockingQueue<>());
        synchronized (subscriptions.get(type)) {
            subscriptions.get(type).offer(m); // Add the subscriber
        }
    }

    @Override
    public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
        subscriptions.putIfAbsent(type, new LinkedBlockingQueue<>());
        synchronized (subscriptions.get(type)) {
            subscriptions.get(type).offer(m); // Add the subscriber
        }
    }

    @Override
    public <T> void complete(Event<T> e, T result) {
        @SuppressWarnings("unchecked")
        Future<T> future = (Future<T>) eventFutures.remove(e);
        if (future != null) {
            future.resolve(result);
        }
    }

    @Override
    public void sendBroadcast(Broadcast b) {
        Class<? extends Broadcast> type = b.getClass();
        LinkedBlockingQueue<MicroService> subscribers;
        synchronized (subscriptions) {
            subscribers = subscriptions.get(type);
        }

        if (subscribers != null) {
            synchronized (subscribers) {
                for (MicroService m : subscribers) {
                    BlockingQueue<Message> queue = microserviceQueues.get(m);
                    if (queue != null) {
                        queue.offer(b);
                    }
                }
            }
        }
    }

    @Override
    public <T> Future<T> sendEvent(Event<T> e) {
        @SuppressWarnings("unchecked")
        Class<? extends Event<T>> type = (Class<? extends Event<T>>) e.getClass();
        LinkedBlockingQueue<MicroService> subscribers;

        synchronized (subscriptions) {
            subscribers = subscriptions.get(type);
        }

        if (subscribers == null || subscribers.isEmpty()) {
            return null;
        }

        MicroService m;
        synchronized (subscribers) {
            m = subscribers.poll();
            if (m != null) {
                subscribers.offer(m);
            }
        }

        if (m != null) {
            BlockingQueue<Message> queue = microserviceQueues.get(m);
            if (queue != null) {
                Future<T> future = new Future<>();
                eventFutures.put(e, future);
                queue.offer(e);
                return future;
            }
        }
        return null;
    }

    @Override
    public void register(MicroService m) {
        System.out.println("Registering MicroService: " + m.getClass().getName());
        microserviceQueues.putIfAbsent(m, new LinkedBlockingQueue<>());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Override
    public void unregister(MicroService m) {
        System.out.println("Unregistering MicroService: " + m.getClass().getName());
        microserviceQueues.remove(m);

        synchronized (subscriptions) {
            for (LinkedBlockingQueue<MicroService> subscribers : subscriptions.values()) {
                synchronized (subscribers) {
                    subscribers.remove(m);
                }
            }
        }

        eventFutures.entrySet().removeIf(entry -> m.equals(entry.getKey()));
    }

    @Override
    public Message awaitMessage(MicroService m) throws InterruptedException {
        BlockingQueue<Message> queue = microserviceQueues.get(m);
        if (queue == null) {
            throw new IllegalStateException("MicroService is not registered.");
        }
        return queue.take();
    }

    public boolean isRegistered(Class<? extends MicroService> type1, MicroService service2) {
        boolean isType1Registered = false;
        boolean isService2QueueEmpty = false;
    
        // Check if any microservice of type1 is still registered
        synchronized (microserviceQueues) {
            for (MicroService microService : microserviceQueues.keySet()) {
                if (type1.isInstance(microService)) {
                    isType1Registered = true;
                    break;
                }
            }
        }
    
        // Check if the message queue of service2 is empty
        BlockingQueue<Message> queue = microserviceQueues.get(service2);
        if (queue != null) {
            isService2QueueEmpty = queue.isEmpty();
        }
    
        // Return true if no type1 is registered and service2's queue is empty
        return !isType1Registered && isService2QueueEmpty;
    }
}
