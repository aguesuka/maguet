package cc.aguesuka.btfind.util;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * @author :aguesuka
 * 2020/2/19 13:58
 */
public class CountMap {
    private Map<String, Long> map = new HashMap<>();

    public long get(String key) {
        return getOrDefaultZero(key);
    }

    public long get(String key, String key2) {
        String k = key + " " + key2;
        return getOrDefaultZero(k);
    }

    private Long getOrDefaultZero(String key) {
        return map.getOrDefault(key, 0L);
    }

    private Long count(String key) {
        return getOrDefaultZero(key) + 1;
    }

    public void put(String key) {
        map.put(key, count(key));
    }

    public void put(String key, String key2) {
        String k = key + " " + key2;
        map.put(k, count(k));
    }

    public void put(String key, String key2, String... keys) {
        StringJoiner joiner = new StringJoiner(" ").add(key).add(key2);
        for (String s : keys) {
            joiner.add(s);
        }
        String k = joiner.toString();
        map.put(k, count(k));
    }

    public Map<String, Long> map() {
        return new HashMap<>(map);
    }

    @Override
    public String toString() {
        Comparator<Map.Entry<String, Long>> comparator =
                Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey());
        return "count     key\n" + map.entrySet().stream()
                .sorted(comparator)
                .map(e -> String.format("%-10d%s", e.getValue(), e.getKey()))
                .collect(Collectors.joining("\n"));
    }
}
