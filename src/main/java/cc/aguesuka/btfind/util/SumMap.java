package cc.aguesuka.btfind.util;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @author :aguesuka
 * 2020/2/19 14:19
 */
public class SumMap {
    final private Map<String, Data> map = new HashMap<>();

    public long getCount(String key) {
        return map.get(key).count;
    }

    public void put(String key, double value) {
        Data data = map.get(key);
        if (data == null) {
            map.put(key, new Data(value));
        } else {
            data.count++;
            data.sum += value;
            data.max = Math.max(data.max, value);
            data.min = Math.min(data.min, value);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SumMap.class.getSimpleName() + "[", "]")
                .add("map=" + map)
                .toString();
    }


    public static class Data {
        private long count;
        private double sum;
        private double max;
        private double min;

        private Data(double data) {
            count = 1;
            sum = data;
            max = data;
            min = data;
        }

        public double max() {
            return max;
        }

        public double min() {
            return min;
        }

        public double sum() {
            return sum;
        }

        public long count() {
            return count;
        }

        public double avg() {
            return sum / count;
        }

        @Override
        public String toString() {
            return new StringJoiner(",", Data.class.getSimpleName() + "[", "]")
                    .add("count=" + count())
                    .add("sum=" + sum())
                    .add("avg=" + avg())
                    .add("max=" + max())
                    .add("min=" + min())
                    .toString();
        }
    }
}
