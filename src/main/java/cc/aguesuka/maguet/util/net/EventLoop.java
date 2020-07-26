package cc.aguesuka.maguet.util.net;

import cc.aguesuka.maguet.util.timer.Timeout;
import cc.aguesuka.maguet.util.timer.Timer;
import cc.aguesuka.maguet.util.timer.WheelTick;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * EventLoop for {@link Selector} and {@link Timer}
 *
 * @author aguesuka
 */
public class EventLoop implements Closeable {
    static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    static final long TICK_DURATION = 100;

    private final Selector selector;
    private final Timer timer;
    private final WheelTick<Runnable> wheelTick;

    private boolean closed = false;

    private EventLoop() throws IOException {
        this.selector = Selector.open();
        wheelTick = new WheelTick<>(TICK_DURATION, TIME_UNIT, 600, System::currentTimeMillis);
        timer = new EventLoopTimer(this, wheelTick);
    }

    /**
     * Start with the task, it equals:
     *
     * @param stater event loop starter
     * @throws IOException          create {@link Selector} failed
     * @throws NullPointerException task is null;
     * @throws EventLoopException   timeout and attachment not {@link Runnable} or throw exception
     */
    public static void start(Stater<EventLoop> stater) throws IOException, EventLoopException {
        try (EventLoop eventLoop = new EventLoop()) {
            stater.run(eventLoop);
            eventLoop.run();
            assert eventLoop.closed : "EventLoop not closed";
        }
    }

    /**
     * Tells whether this channel closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    private boolean shouldShutdown() {
        if (closed) {
            return true;
        }
        if (Thread.currentThread().isInterrupted()) {
            close();
            return true;
        }
        return false;
    }

    private void executeTimeoutTask() {
        if (shouldShutdown()) {
            return;
        }
        wheelTick.tick(TIME_UNIT);
        List<Runnable> timeoutTasks = wheelTick.take();
        for (Runnable task : timeoutTasks) {
            if (shouldShutdown()) {
                return;
            }
            try {
                task.run();
            } catch (Throwable e) {
                throw new EventLoopException("timeout task exception", e);
            }

        }
    }

    private void select() throws IOException {
        if (shouldShutdown()) {
            return;
        }
        long nextTickLeft = wheelTick.tick(TIME_UNIT);
        selector.select(Math.max(0, nextTickLeft));
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        ArrayList<SelectionKey> keys = new ArrayList<>(selectedKeys);
        selectedKeys.clear();

        for (SelectionKey key : keys) {
            if (shouldShutdown()) {
                return;
            }

            Object attachment = key.attachment();
            if (attachment == null) {
                throw new EventLoopException("attachment is null");
            }
            if (!(attachment instanceof Runnable)) {
                throw new EventLoopException("attachment not java.lang.Runnable");
            }

            Runnable callback = (Runnable) attachment;
            try {
                callback.run();
            } catch (Throwable e) {
                throw new EventLoopException("callback exception", e);
            }
        }
    }

    private void run() throws IOException, EventLoopException {
        while (!shouldShutdown()) {
            executeTimeoutTask();
            select();
        }
    }

    /**
     * Get the timer
     *
     * @return timer
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * Register the Channel on the EventLoop.
     *
     * @param channel  channel
     * @param ops      ops
     * @param callback Select callback
     * @return SelectionKey
     * @throws ClosedChannelException if Channel closed
     * @throws EventLoopException     if EventLoop closed
     */
    public SelectionKey register(SelectableChannel channel, int ops, Runnable callback) throws ClosedChannelException {
        Objects.requireNonNull(callback, "callback is null");
        if (closed) {
            throw new EventLoopException("closed");
        }
        return channel.register(selector, ops, callback);
    }


    /**
     * Close this EventLoop
     * <p>If the selector has already been closed then this method returns immediately. Otherwise, clear the {@link
     * Timer} , {@link Selector} and all {@link SelectableChannel} in selector</p>
     *
     * @throws BatchCloseException If close Selector or SelectableChannel failed
     */
    @Override
    public void close() throws BatchCloseException {
        if (closed) {
            return;
        }
        closed = true;

        wheelTick.take();
        wheelTick.clearUnexpired();

        Set<SelectionKey> keys = selector.keys();
        List<SelectionKey> keysCopy = new ArrayList<>(keys);
        List<Throwable> ioExceptionList = new ArrayList<>();
        tryDo(ioExceptionList, selector::selectNow);
        tryDo(ioExceptionList, selector::close);
        for (SelectionKey key : keysCopy) {
            SelectableChannel channel = key.channel();
            tryDo(ioExceptionList, channel::close);
        }
        if (!ioExceptionList.isEmpty()) {
            throw new BatchCloseException(ioExceptionList);
        }
    }

    private void tryDo(Collection<? super Throwable> throwableList, ThrowableRunnable action) {
        try {
            action.run();
        } catch (Throwable throwable) {
            throwableList.add(throwable);
        }
    }

    @FunctionalInterface
    private interface ThrowableRunnable {
        void run() throws Throwable;
    }

    @FunctionalInterface
    public interface Stater<T> {
        void run(T t) throws IOException;
    }

    private static class EventLoopTimer implements Timer {
        private final WheelTick<Runnable> wheelTick;
        private final EventLoop eventLoop;

        public EventLoopTimer(EventLoop eventLoop, WheelTick<Runnable> wheelTick) {
            this.wheelTick = wheelTick;
            this.eventLoop = eventLoop;
        }


        @Override
        public Timeout createTimeout(Runnable task, long delay, TimeUnit timeUnit) {
            if (eventLoop.closed) {
                throw new EventLoopException("closed");
            }
            return wheelTick.add(task, delay, timeUnit);
        }
    }
}
