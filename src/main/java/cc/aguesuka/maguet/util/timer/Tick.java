package cc.aguesuka.maguet.util.timer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A data structure of save objects with the delay.
 * <p>
 * in which an element can only be taken when it's delay has expired. on every tick, will check if there are any element
 * behind the schedule and make it expire.
 *
 * @param <E> type of elements
 * @author aguesuka
 */
public interface Tick<E> {
    /**
     * Clear all unexpired elements
     *
     * @return unexpired elements
     */
    List<E> clearUnexpired();


    /**
     * Gets expired elements, call {@link #tick} before take
     *
     * @return expired elements of this duration or empty list
     */
    Collection<E> take();


    /**
     * Ticks if now >= nextTickTime, return next tick time left
     *
     * @param timeUnit timeUnit of result
     * @return next tick time  - now
     */
    long tick(TimeUnit timeUnit);

    /**
     * Gets size of unexpired elements
     *
     * @return size
     */
    int unexpiredSize();

    /**
     * Adds the element with delay
     *
     * @param element  the element
     * @param delay    delay time
     * @param timeUnit time unit of {@code delay}
     * @return a key {@link Timeout}, can cancel the element before expired
     * @throws IllegalArgumentException delay < 0 or delay too long
     */
    Timeout add(E element, long delay, TimeUnit timeUnit);
}
