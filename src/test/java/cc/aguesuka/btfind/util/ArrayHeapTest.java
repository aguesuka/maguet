package cc.aguesuka.btfind.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author :aguesuka
 * 2019/12/28 16:16
 */
public class ArrayHeapTest {
    private ArrayHeap<Long> arrayHeap;
    private Random random = new SecureRandom();

    @Before
    public void before() {
        arrayHeap = new ArrayHeap<>(16);
    }

    private <E extends Comparable<E>> List<E> toListAndSort(ArrayHeap<E> arrayHeap) {
        int size = arrayHeap.size();
        ArrayList<E> list = new ArrayList<>(size);
        arrayHeap.foreach(list::add);
        Assert.assertEquals(list.size(), size);
        Collections.sort(list);
        return list;
    }

    @Test
    public void addOnly() {

        int endExclusive = 10000;
        List<Long> collect = LongStream.range(0, endExclusive).peek(i -> {
            Assert.assertEquals(i, arrayHeap.size());
            arrayHeap.add(i);
        }).boxed().collect(Collectors.toList());
        Assert.assertEquals(collect, toListAndSort(arrayHeap));
    }

    @Test
    public void addAndRemove() {
        int endExclusive = 10000;
        Map<Long, Integer> longIndex = new HashMap<>();

        List<Long> testList = new ArrayList<>();
        LongStream.range(0, endExclusive).boxed().forEach(i -> {
            testList.add(i);
            int index = arrayHeap.add(i);
            longIndex.put(i, index);

            Assert.assertEquals(testList.size(), arrayHeap.size());
            int indexForRemove = random.nextInt(arrayHeap.size() * 10);
            while (indexForRemove < testList.size()) {
                Long remove = testList.remove(indexForRemove);
                arrayHeap.remove(longIndex.get(remove), remove);
            }

            Assert.assertEquals(testList.size(), arrayHeap.size());
        });
        Assert.assertEquals(testList, toListAndSort(arrayHeap));
    }
}