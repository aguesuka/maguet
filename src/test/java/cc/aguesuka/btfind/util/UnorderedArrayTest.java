package cc.aguesuka.btfind.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author :yangmingyuxing
 * 2019/12/28 16:16
 */
public class UnorderedArrayTest {
    private UnorderedArray<Long> unorderedArray;
    private Random random = new SecureRandom();

    @Before
    public void before() {
        unorderedArray = new UnorderedArray<>(16);
    }

    private <E extends Comparable<E>> List<E> toListAndSort(UnorderedArray<E> unorderedArray) {
        int size = unorderedArray.size();
        ArrayList<E> list = new ArrayList<>(size);
        unorderedArray.foreach(list::add);
        Assert.assertEquals(list.size(), size);
        Collections.sort(list);
        return list;
    }

    @Test
    public void addOnly() {

        int endExclusive = 10000;
        List<Long> collect = LongStream.range(0, endExclusive).peek(i -> {
            Assert.assertEquals(i, unorderedArray.size());
            unorderedArray.add(i);
        }).boxed().collect(Collectors.toList());
        Assert.assertEquals(collect, toListAndSort(unorderedArray));
    }

    @Test
    public void addAndRemove() {
        int endExclusive = 10000;
        Map<Long, Integer> longIndex = new HashMap<>();

        List<Long> testList = new ArrayList<>();
        LongStream.range(0, endExclusive).boxed().forEach(i -> {
            testList.add(i);
            int index = unorderedArray.add(i);
            longIndex.put(i, index);

            Assert.assertEquals(testList.size(), unorderedArray.size());
            int indexForRemove = random.nextInt(unorderedArray.size() * 10);
            while (indexForRemove < testList.size()) {
                Long remove = testList.remove(indexForRemove);
                unorderedArray.remove(longIndex.get(remove), remove);
            }

            Assert.assertEquals(testList.size(), unorderedArray.size());
        });
        Assert.assertEquals(testList, toListAndSort(unorderedArray));
    }
}