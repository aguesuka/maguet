package cc.aguesuka.btfind.util;


import java.util.*;
import java.util.function.BiConsumer;

/**
 * Lazy timeout not thread safe map,time unit is Millis. Value must not null.
 * check timeout at call {@link TimeoutMap#refresh()}, {@link TimeoutMap#get(K)}, {@link TimeoutMap#getAndRemove(K)}
 *
 * @param <K> key
 * @param <V> value
 * @author :yangmingyuxing
 * 2020/2/14 10:54
 */
public class TimeoutMap<K, V> {
    private Map<K, TimeoutEntry<K, V>> map = new HashMap<>();

    @Override
    public String toString() {
        return new StringJoiner(", ", TimeoutMap.class.getSimpleName() + "[", "]")
                .add("map=" + map)
                .add("size=" + size())
                .toString();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * put key/value with timeout without timeout callback
     *
     * @param key     key
     * @param value   value
     * @param timeout timeout,time unit is Millis.
     */
    public void put(K key, V value, long timeout) {
        put(key, value, timeout, null);
    }

    /**
     * map size. Actually size is letter. call {@link TimeoutMap#refresh} first to get actually size
     *
     * @return size
     */
    public int size() {
        return map.size();
    }

    /**
     * put with timeout and timeout callback
     *
     * @param key      key
     * @param value    value
     * @param timeout  timeout,time unit is Millis.
     * @param callback timeout callback
     */
    public void put(K key, V value, long timeout, BiConsumer<K, V> callback) {
        Objects.requireNonNull(value);
        getAndRemove(key);
        map.put(key, new TimeoutEntry<>(key, value, now() + timeout, callback));
    }

    /**
     * get value by key.if not containsKey or value if timeout.return null
     *
     * @param key key
     * @return value
     */
    public V get(K key) {
        TimeoutEntry<K, V> value = map.get(key);
        V v = checkTimeout(value, now());
        if (v == null && value != null) {
            map.remove(key);
        }
        return v;
    }

    private V checkTimeout(TimeoutEntry<K, V> timeoutEntry, long now) {
        // return null if timeoutEntry is null or is timeout
        if (timeoutEntry == null) {
            return null;
        }
        boolean isTimeout = timeoutEntry.effectiveTime < now;
        if (isTimeout) {
            if (timeoutEntry.callback != null) {
                // call callback.make sure .remove entry when this method return null
                timeoutEntry.callback.accept(timeoutEntry.key, timeoutEntry.value);
                timeoutEntry.callback = null;
                timeoutEntry.value = null;
            }
            return null;
        }

        return timeoutEntry.value;
    }

    /**
     * get value by key,remove it
     *
     * @param key key
     * @return value if contains key and not timeout.or null else.
     */
    public V getAndRemove(K key) {
        TimeoutEntry<K, V> timeoutEntry = map.remove(key);
        return checkTimeout(timeoutEntry, now());
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * remove all timeout entry.
     */
    public void refresh() {
        long now = now();
        map.entrySet().removeIf(e -> checkTimeout(e.getValue(), now) == null);
    }

    private static class TimeoutEntry<K, V> {
        K key;
        V value;
        long effectiveTime;
        BiConsumer<K, V> callback;

        TimeoutEntry(K key, V value, long effectiveTime, BiConsumer<K, V> callback) {
            this.key = key;
            this.value = value;
            this.effectiveTime = effectiveTime;
            this.callback = callback;
        }

        @Override
        public String toString() {
            return Objects.toString(value);
        }
    }

}
