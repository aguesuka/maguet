package cc.aguesuka.btfind.util;

import java.util.StringJoiner;

/**
 * @author :aguesuka
 * 2020/2/19 14:30
 */
public class NumberLogger {
    private CountMap countMap = new CountMap();
    private SumMap sumMap = new SumMap();

    public CountMap getCountMap() {
        return countMap;
    }


    public SumMap getSumMap() {
        return sumMap;
    }

    @Override
    public String toString() {
        StringJoiner stringJoiner = new StringJoiner("\n");
        return stringJoiner.add("class numberLogger")
                .add(countMap.toString())
                .add(sumMap.toString())
                .toString();

    }
}
