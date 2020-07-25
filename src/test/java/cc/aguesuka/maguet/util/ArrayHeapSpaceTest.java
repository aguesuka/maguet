package cc.aguesuka.maguet.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * @author :aguesuka
 * 2019/12/28 16:16
 */
public class ArrayHeapSpaceTest {
    private ArrayHeapSpace<Long> arrayHeapSpace;
    private final Random random = new SecureRandom();

    @Before
    public void before() {
        arrayHeapSpace = new ArrayHeapSpace<>();
    }

    private <E extends Comparable<E>> List<E> toListAndSort(ArrayHeapSpace<E> arrayHeapSpace) {
        int size = arrayHeapSpace.size();
        ArrayList<E> list = new ArrayList<>(size);
        arrayHeapSpace.forEach(list::add);
        Assert.assertEquals(list.size(), size);
        Collections.sort(list);
        return list;
    }

    @Test
    public void testAddOnly() {

        int endExclusive = 10000;
        List<Long> collect = LongStream.range(0, endExclusive).peek(i -> {
            Assert.assertEquals(i, arrayHeapSpace.size());
            int index = arrayHeapSpace.add(i);
            assert index == i;
        }).boxed().collect(Collectors.toList());
        Assert.assertEquals(collect, toListAndSort(arrayHeapSpace));
    }

    @Test
    public void testAddAndRemove() {
        int endExclusive = 10000;
        Map<Long, Integer> longIndex = new HashMap<>();

        List<Long> testList = new ArrayList<>();
        LongStream.range(0, endExclusive).boxed().forEach(i -> {
            testList.add(i);
            int index = arrayHeapSpace.add(i);
            longIndex.put(i, index);

            Assert.assertEquals(testList.size(), arrayHeapSpace.size());
            int indexForRemove = random.nextInt(arrayHeapSpace.size() * 10);
            while (indexForRemove < testList.size()) {
                Long remove = testList.remove(indexForRemove);
                arrayHeapSpace.remove(longIndex.get(remove), remove);
                assert arrayHeapSpace.size() == testList.size();
            }
        });
        assert arrayHeapSpace.size() == testList.size();
        Assert.assertEquals(testList, toListAndSort(arrayHeapSpace));
    }

    @Test
    public void spliterator() {
        long[] intArray = LongStream.range(0, 100).map(i -> random.nextInt())
                .peek(arrayHeapSpace::add)
                .toArray();
        long[] arrayHeapSpaceArray =
                StreamSupport.stream(arrayHeapSpace.spliterator(), false).mapToLong(i -> i).toArray();
        Assert.assertArrayEquals(intArray, arrayHeapSpaceArray);
    }
}