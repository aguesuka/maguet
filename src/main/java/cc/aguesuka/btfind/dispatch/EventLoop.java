package cc.aguesuka.btfind.dispatch;

import cc.aguesuka.btfind.util.TimeoutListener;
import cc.aguesuka.btfind.util.WheelTimer;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author :yangmingyuxing
 * 2019/12/29 18:04
 */
public class EventLoop implements Closeable {
    private WheelTimer wheelTimer;
    private Selector selector;
    private volatile boolean start;
    private volatile boolean closed;

    private EventLoop() throws IOException {
        this.wheelTimer = new WheelTimer(100, 6000, 1000);
        selector = Selector.open();
    }

    public static void run(Bootstrap bootstrap) {
        try {
            EventLoop eventLoop = new EventLoop();
            bootstrap.run(eventLoop);
            eventLoop.startLoop();
        } catch (Exception e) {
            throw new RuntimeException("event loop top Exception", e);
        }
    }

    private void startLoop() throws IOException {
        start = true;
        try (this) {
            loop();
        }
    }

    public SelectionKey register(SelectableChannel channel, int ops, Handler handler) throws ClosedChannelException {
        Objects.requireNonNull(handler);
        return channel.register(selector, ops, handler);
    }


    public void interval(long delay, BooleanSupplier checker, TimeoutListener callback) {
        timeout(0, new TimeoutListener() {
            @Override
            public void timeout() {
                if (checker.getAsBoolean()) {
                    callback.timeout();
                    EventLoop.this.timeout(delay, this);
                }
            }

            @Override
            public String toString() {
                return "EventLoop$interval, delay=" + delay;
            }
        });
    }

    public long timeout(long delay, TimeoutListener callback) {
        if (closed) {
            return 0;
        }
        return wheelTimer.add(delay, callback);
    }

    public void cancelTimeout(long timeoutIndex, TimeoutListener callback) {
        if (!closed) {
            wheelTimer.remove(timeoutIndex, callback);
        }
    }


    private void loop() throws IOException {
        Consumer<SelectionKey> action = this::handle;
        while (start) {
            wheelTimer.tick();
            if (start) {
                long nextTickLeft = wheelTimer.nextTickLeft();
                try {
                    if (nextTickLeft > 0) {
                        selector.select(action, nextTickLeft);
                    } else {
                        selector.selectNow(action);
                    }
                } catch (ClosedSelectorException e) {
                    if (start) {
                        throw e;
                    }
                    // if not start, ignore
                    System.out.println("event loop closed");
                }
            }
        }
    }

    private void handle(SelectionKey key) {
        if (!key.isValid()) {
            return;
        }
        Object attachment = key.attachment();
        if (!(attachment instanceof Handler)) {
            throw new AssertionError();
        }
        Handler handler = (Handler) attachment;
        // any exception will stop event loop
        handler.handle();
    }

    @Override
    public void close() {
        start = false;
        closed = true;
        wheelTimer = null;
        Selector selector = this.selector;
        this.selector = null;
        if (selector == null) {
            return;
        }

        Set<SelectionKey> keys = selector.keys();

        List<Exception> errorList = new ArrayList<>();
        tryClose(selector, errorList);
        for (SelectionKey key : keys) {
            if (key != null) {
                tryClose(key.channel(), errorList);
                tryClose(key.attachment(), errorList);
            }
        }


        if (!errorList.isEmpty()) {
            UncheckedIOException exception = new UncheckedIOException(new IOException("exception on close EventLoop"));
            for (Exception e : errorList) {
                exception.addSuppressed(e);
            }
            throw exception;
        }

    }

    private void tryClose(Object closeable, Collection<Exception> exceptions) {
        try {
            if (closeable instanceof Closeable) {
                ((Closeable) closeable).close();
            }

        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    public interface Handler {
        void handle();
    }

    @FunctionalInterface
    public interface Bootstrap {
        void run(EventLoop eventLoop) throws Exception;
    }
}
