package cc.aguesuka.btfind.util.timer;

import cc.aguesuka.btfind.util.ArrayHeapSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * WheelTick is tick implement by wheel.
 * <p>
 * Every {@link #tickDuration} time passed, implicit call {@link #doTick} will take the
 * next cell in {@link #wheel}, and make the elements expired. expired elements can get by {@link #take},
 * expired is not on time: {@code delay <= theActuallyExpiredTime <= delay + tickDuration}.
 * <p>
 * the operation {@link #doTick} cost linear-time bad, but  cast constant-time average.
 * the operation {@link #add} and  {@link #doCancel}  called {@link #doTick},and cost
 * constant-time average. operation {@link #take} cost constant-time.
 *
 * @author :aguesuka
 * 2020/4/8 18:36
 */
public class WheelTick<E> implements Tick<E> {
    // region setting fields
    final TimeUnit timeUnit;
    final long tickDuration;
    /**
     * the max time of the delay,
     * {@code wheelDuration = tickDuration * ticksPerWheel}
     */
    final long wheelDuration;
    private final int ticksPerWheel;
    private final LongSupplier currentTimeGetter;
    // endregion


    // region state fields
    private final ArrayHeapSpace<WheelTickTimeout<E>>[] wheel;

    private long nextTickTime;
    private int nextReadyIndex;
    private List<E> readyTaskList;
    private int unexpiredSize;
    private long lastCheckTime;
    // endregion


    /**
     * create a wheel tick
     *
     * @param tickDuration      the tick duration. maxDelayTime = tickDuration * ticksPerWheel.
     * @param timeUnit          the time unit of the {@code tickDuration} and the {@code currentTimeGetter}
     * @param ticksPerWheel     the size of the wheel
     * @param currentTimeGetter clock {@link System#currentTimeMillis} or {@link System#nanoTime} or other
     * @throws NullPointerException     any param is null
     * @throws IllegalArgumentException {@code tickDuration} or {@code ticksPerWheel} is <=0.
     */
    public WheelTick(long tickDuration, TimeUnit timeUnit, int ticksPerWheel, LongSupplier currentTimeGetter) {
        this.timeUnit = Objects.requireNonNull(timeUnit);
        this.tickDuration = tickDuration;
        this.ticksPerWheel = ticksPerWheel;
        this.currentTimeGetter = Objects.requireNonNull(currentTimeGetter);
        this.wheelDuration = tickDuration * ticksPerWheel;
        check();

        ArrayHeapSpace<?>[] arrayHeapSpaces = new ArrayHeapSpace[ticksPerWheel];
        @SuppressWarnings("unchecked")
        ArrayHeapSpace<WheelTickTimeout<E>>[] wheel = (ArrayHeapSpace<WheelTickTimeout<E>>[]) arrayHeapSpaces;
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new ArrayHeapSpace<>();
        }
        this.wheel = wheel;

        long now = now();
        lastCheckTime = now;
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


    /**
     * {@inheritDoc}
     * <p>
     * will unlink all {@link WheelTickTimeout}
     *
     * @return elements unexpired
     */
    @Override
    public List<E> clearUnexpired() {
        List<E> result = new ArrayList<>(unexpiredSize);
        for (int i = 0; i < wheel.length; i++) {
            ArrayHeapSpace<WheelTickTimeout<E>> cell = wheel[i];
            wheel[i] = new ArrayHeapSpace<>();
            for (WheelTickTimeout<E> wheelTickTimeout : cell) {
                result.add(wheelTickTimeout.take());
            }
            cell.clear();
        }
        unexpiredSize = 0;
        nextReadyIndex = 0;
        nextTickTime = now() + tickDuration;
        return result;
    }


    @Override
    public List<E> take() {
        List<E> result = Objects.requireNonNullElse(readyTaskList, Collections.emptyList());
        readyTaskList = null;
        return result;
    }

    @Override
    public long tick(TimeUnit timeUnit) {
        long now = doTick();
        return timeUnit.convert(nextTickTime - now, this.timeUnit);
    }

    public int unexpiredSize() {
        return unexpiredSize;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException delay is < 0 or delay time is more than {@link #wheelDuration}
     * @implNote add spend constant time
     */
    @Override
    public Timeout add(E element, long delay, TimeUnit timeUnit) {
        if (delay < 0) {
            throw new IllegalArgumentException("delay < 0");
        }
        long now = doTick();
        long readyTimeAfter = now + this.timeUnit.convert(delay, timeUnit);
        int durationCount = (int) ((readyTimeAfter - nextTickTime + tickDuration) / tickDuration);
        if (durationCount >= ticksPerWheel) {
            throw new IllegalArgumentException("delay too long: " + delay);
        }
        int index = durationCount + nextReadyIndex;
        int indexOfWheel = index < ticksPerWheel ? index : index - ticksPerWheel;

        WheelTickTimeout<E> timeout = new WheelTickTimeout<>(indexOfWheel, element, this);
        timeout.indexOfCell = wheel[timeout.indexOfWheel].add(timeout);
        unexpiredSize++;
        return timeout;

    }


    private boolean doCancel(WheelTickTimeout<E> timeout) {
        doTick();
        ArrayHeapSpace<WheelTickTimeout<E>> cell = wheel[timeout.indexOfWheel];
        boolean success = cell.remove(timeout.indexOfCell, timeout);
        if (success) {
            unexpiredSize--;
            timeout.unlink();
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

            ArrayHeapSpace<WheelTickTimeout<E>> cell = wheel[nextReadyIndex];
            wheel[nextReadyIndex] = new ArrayHeapSpace<>();

            unexpiredSize -= cell.size();
            for (WheelTickTimeout<E> timeout : cell) {
                readyTaskList.add(timeout.take());
            }
            cell.clear();
            nextReadyIndex = (nextReadyIndex + 1) % ticksPerWheel;
            nextTickTime += tickDuration;
        }
        return now;
    }

    private boolean nowIsLaterNextTickTime(long now) {
        long subNext = now - nextTickTime;
        if (subNext >= wheelDuration || now - lastCheckTime < 0) {
            if (readyTaskList == null) {
                readyTaskList = clearUnexpired();
            } else {
                readyTaskList.addAll(clearUnexpired());
            }
            return false;
        }
        lastCheckTime = now;
        return subNext >= 0;
    }

    private static class WheelTickTimeout<E> implements Timeout {
        final private int indexOfWheel;
        private int indexOfCell;
        private E element;
        private WheelTick<E> wheelTick;

        public WheelTickTimeout(int indexOfWheel, E element, WheelTick<E> wheelTick) {
            this.indexOfWheel = indexOfWheel;
            this.element = element;
            this.wheelTick = wheelTick;
        }

        private void unlink() {
            element = null;
            wheelTick = null;
        }

        private E take() {
            assert wheelTick != null;
            E element = this.element;
            unlink();
            return element;
        }

        @Override
        public boolean cancel() {
            if (this.wheelTick == null) {
                return false;
            }
            return wheelTick.doCancel(this);
        }
    }
}
