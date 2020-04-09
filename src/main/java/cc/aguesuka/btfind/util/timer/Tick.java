package cc.aguesuka.btfind.util.timer;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @param <E> type of elements
 * @author :aguesuka
 * 2020/4/5 12:59
 */
public interface Tick<E> {
    /**
     * clear all elements
     *
     * @return not ready elements
     */
    List<E> clear();


    /**
     * get ready elements
     *
     * @return ready elements of this duration or empty list
     */
    List<E> take();


    /**
     * get next tick time left
     *
     * @param timeUnit timeUnit of result
     * @return next tick time  - now, zero if has ready elements.
     */
    long nextTickLeft(TimeUnit timeUnit);

    /**
     * get size of not ready elements
     *
     * @return size
     */
    int size();

    Timeout add(E element, long delay, TimeUnit timeUnit);

    interface CurrentTimeGetter {
        long currentTime();
    }
}
