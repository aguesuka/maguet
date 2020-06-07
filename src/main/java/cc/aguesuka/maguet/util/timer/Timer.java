package cc.aguesuka.maguet.util.timer;

import java.util.concurrent.TimeUnit;

/**
 * Timer
 *
 * @author :aguesuka
 * 2020/4/2 18:00
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
}
