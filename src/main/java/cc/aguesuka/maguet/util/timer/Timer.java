package cc.aguesuka.maguet.util.timer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Timer
 *
 * @author aguesuka
 */
public interface Timer {
    /**
     * Schedules the specified task for execution after the specified delay
     *
     * @param task     task to be scheduled.
     * @param delay    delay in timeUnit before task is to be executed.
     * @param timeUnit timeUnit of delay
     * @return Timeout of this schedules
     */
    Timeout createTimeout(Runnable task, long delay, TimeUnit timeUnit);

    /**
     * Creates timeout by duration
     *
     * @param task  task to be scheduled
     * @param delay delay time
     * @return Timeout of this schedules
     * @see #createTimeout(Runnable, long, TimeUnit)
     */
    default Timeout createTimeout(Runnable task, Duration delay) {
        return createTimeout(task, delay.toNanos(), TimeUnit.NANOSECONDS);
    }
}
