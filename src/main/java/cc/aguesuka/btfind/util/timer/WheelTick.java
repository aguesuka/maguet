package cc.aguesuka.btfind.util.timer;

import cc.aguesuka.btfind.util.ArrayHeapSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * @author :aguesuka
 * 2020/4/8 18:36
 */
public class WheelTick<E> implements Tick<E> {
    // region setting fields
    final TimeUnit timeUnit;
    final long tickDuration;
    final long wheelDuration;
    private final int ticksPerWheel;
    private final LongSupplier currentTimeGetter;
    // endregion
    // region state fields
    private final ArrayHeapSpace<WheelTickTimeout>[] wheel;

    private long nextTickTime;
    private int nextReadyIndex;
    private List<E> readyTaskList;
    private int size;
    private long lastTickTime;
    // endregion

    public WheelTick(long tickDuration, TimeUnit timeUnit, int ticksPerWheel, LongSupplier currentTimeGetter) {
        this.timeUnit = Objects.requireNonNull(timeUnit);
        this.tickDuration = tickDuration;
        this.ticksPerWheel = ticksPerWheel;
        this.currentTimeGetter = Objects.requireNonNull(currentTimeGetter);
        this.wheelDuration = tickDuration * ticksPerWheel;
        check();
        @SuppressWarnings("unchecked")
        ArrayHeapSpace<WheelTickTimeout>[] wheel =
                (ArrayHeapSpace<WheelTickTimeout>[]) new ArrayHeapSpace[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new ArrayHeapSpace<>();
        }
        this.wheel = wheel;
        long now = now();
        lastTickTime = now;
        this.nextTickTime = tickDuration + now;
    }


    private void check() {
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel <= 0");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration <= 0");
        }
        if (((Long.MAX_VALUE / 4) / tickDuration) <= ticksPerWheel) {
            throw new IllegalArgumentException("tickDuration * ticksPerWheel >= Long.MAX_VALUE / 4");
        }
    }


    @Override
    public List<E> clear() {
        List<E> result = new ArrayList<>(size);
        for (int i = 0; i < wheel.length; i++) {
            ArrayHeapSpace<WheelTickTimeout> cell = wheel[i];
            wheel[i] = new ArrayHeapSpace<>();
            for (WheelTickTimeout wheelTickTimeout : cell) {
                result.add(wheelTickTimeout.element);
            }
            cell.clear();
        }
        size = 0;
        nextReadyIndex = 0;
        nextTickTime = now() + tickDuration;
        readyTaskList = null;
        return result;
    }


    @Override
    public List<E> take() {
        List<E> result = Objects.requireNonNullElse(readyTaskList, Collections.emptyList());
        readyTaskList = null;
        return result;
    }

    @Override
    public long nextTickLeft(TimeUnit timeUnit) {
        long now = doTick();
        return timeUnit.convert(nextTickTime - now, this.timeUnit);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Timeout add(E element, long delay, TimeUnit timeUnit) {
        if (delay < 0) {
            throw new IllegalArgumentException("delay < 0");
        }
        long now = doTick();
        long readyTimeAfter = now + this.timeUnit.convert(delay, timeUnit);
        int durationCount = (int) ((readyTimeAfter - nextTickTime) / tickDuration) + 1;
        if (durationCount >= ticksPerWheel) {
            throw new IllegalArgumentException("delay too long: " + delay);
        }
        int index = durationCount + nextReadyIndex;
        int indexOfWheel = index < ticksPerWheel ? index : index - ticksPerWheel;

        WheelTickTimeout timeout = new WheelTickTimeout();
        timeout.indexOfWheel = indexOfWheel;
        timeout.element = element;
        timeout.indexOfCell = wheel[timeout.indexOfWheel].add(timeout);
        size++;
        return timeout;

    }


    private boolean doCancel(WheelTickTimeout timeout) {
        ArrayHeapSpace<WheelTickTimeout> cell = wheel[timeout.indexOfWheel];
        boolean success = cell.remove(timeout.indexOfCell, timeout);
        if (success) {
            size--;
            timeout.element = null;
        }
        return success;
    }

    private long now() {
        return currentTimeGetter.getAsLong();
    }

    /**
     * take the ready tasks into {@link #readyTaskList}
     *
     * @return now
     */
    private long doTick() {
        long now;
        while (nowIsLaterNextTickTime(now = now())) {
            if (readyTaskList == null) {
                readyTaskList = new ArrayList<>(wheel[nextReadyIndex].size());
            }
            // Normally it will only loop once

            ArrayHeapSpace<WheelTickTimeout> cell = wheel[nextReadyIndex];
            wheel[nextReadyIndex] = new ArrayHeapSpace<>();

            size -= cell.size();
            for (WheelTickTimeout timeout : cell) {
                readyTaskList.add(timeout.element);
            }
            cell.clear();
            nextReadyIndex = (nextReadyIndex + 1) % ticksPerWheel;
            nextTickTime += tickDuration;
        }

        return now;
    }

    private boolean nowIsLaterNextTickTime(long now) {
        long subNext = now - nextTickTime;
        if (subNext >= wheelDuration || now - lastTickTime < 0) {
            if (readyTaskList == null) {
                readyTaskList = clear();
            } else {
                readyTaskList.addAll(clear());
            }
            return false;
        }
        lastTickTime = now;

        return subNext >= 0;
    }

    private class WheelTickTimeout implements Timeout {
        private int indexOfWheel;
        private int indexOfCell;
        private E element;


        @Override
        public boolean cancel() {
            return doCancel(this);
        }
    }
}
