package cc.aguesuka.maguet.util.timer;

import java.util.concurrent.TimeUnit;

/**
 * @author :aguesuka
 * 2020/4/2 18:00
 */
public interface Timer {
    Timeout createTimeout(Runnable task, long delay, TimeUnit timeUnit);
}
