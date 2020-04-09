package cc.aguesuka.btfind.util;

/**
 * @author :aguesuka
 * 2019/12/28 21:37
 */
public class WheelTimer {
    private long tickDuration;
    private ArrayHeapSpace<TimeoutListener>[] wheel;
    private long nextTickTime;
    private int tickIndex;
    private boolean ticking;
    public WheelTimer(long tickDuration, int ticksPerWheel) {
        if (tickDuration <= 0 || ticksPerWheel <= 0) {
            throw new IllegalArgumentException("tickDuration and ticksPerWheel must > 0");
        }
        if (Long.MAX_VALUE / ticksPerWheel <= tickDuration) {
            throw new IllegalArgumentException("tickDuration or ticksPerWheel too large");
        }
        this.tickDuration = tickDuration;
        @SuppressWarnings("unchecked")
        ArrayHeapSpace<TimeoutListener>[] wheel = new ArrayHeapSpace[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new ArrayHeapSpace<>();
        }
        this.wheel = wheel;
        nextTickTime = now() + nextTickTime;
    }


    public long add(long delay, TimeoutListener timeoutListener) {
        if (!ticking) {
            tick();
        }
        if (delay < 0) {
            throw new IllegalArgumentException("delay must >= 0");
        }
        long delayTicks = delay / tickDuration;
        int wheelLength = wheel.length;
        if (delayTicks >= wheelLength) {
            throw new IllegalArgumentException("delay too long");
        }
        int offset = (int) delayTicks + tickIndex;

        int wheelIndex = (offset < wheelLength) ? offset : (offset - wheelLength);
        assert wheelIndex == offset % wheelLength;
        int bucketIndex = wheel[wheelIndex].add(timeoutListener);
        return ((long) wheelIndex << 32 | (0xffffffffL & (long) bucketIndex));
    }

    public void remove(long index, TimeoutListener timeoutListener) {
        int wheelIndex = (int) (index >> 32);
        int bucketIndex = (int) index;
        if (wheelIndex < 0 || bucketIndex < 0 || wheelIndex > wheel.length) {
            return;
        }
        ArrayHeapSpace<TimeoutListener> bucket = wheel[wheelIndex];
        bucket.remove(bucketIndex, timeoutListener);
    }

    public int tick() {
        ticking = true;
        int result = 0;
        while (nextTickLeft() <= 0) {
            ArrayHeapSpace<TimeoutListener> bucket = wheel[tickIndex];
            wheel[tickIndex] = new ArrayHeapSpace<>();
            incrementTickIndex();

            bucket.forEach(TimeoutListener::timeout);
            result += bucket.size();
            bucket.clear();
        }
        ticking = false;
        return result;
    }

    public long nextTickLeft() {
        return nextTickTime - now();
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private void incrementTickIndex() {
        tickIndex++;
        if (tickIndex == wheel.length) {
            tickIndex = 0;
        }
        nextTickTime += tickDuration;
    }
}
