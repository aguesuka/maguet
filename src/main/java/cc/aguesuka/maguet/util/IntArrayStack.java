package cc.aguesuka.maguet.util;

import java.util.*;

/**
 * warp int array as a stack
 *
 * @author :aguesuka
 * 2020/4/1 18:33
 */
public class IntArrayStack implements Iterable<Integer> {
    private static final int DEFAULT_SIZE = 16;
    private static final int[] EMPTY_ARRAY = new int[0];
    private int[] elements;
    private int size;

    public IntArrayStack() {
        this.elements = EMPTY_ARRAY;
        this.size = 0;
    }

    private static int nextCapacity(int currentCapacity) {
        if (currentCapacity <= DEFAULT_SIZE / 2) {
            return DEFAULT_SIZE;
        } else if (currentCapacity == Integer.MAX_VALUE) {
            throw new OutOfMemoryError();
        } else if (currentCapacity > Integer.MAX_VALUE / 2) {
            return Integer.MAX_VALUE;
        }
        return currentCapacity * 2;
    }

    /**
     * add at last
     *
     * @param i an int
     */
    public void push(int i) {
        size++;
        if (size >= elements.length) {
            grow();
        }
        elements[size - 1] = i;
    }

    /**
     * get last value
     *
     * @return last value
     */
    public int pop() {
        if (size <= 0) {
            throw new NoSuchElementException();
        }
        size--;
        return elements[size];
    }

    /**
     * remove all value
     */
    public void clear() {
        size = 0;
    }

    /**
     * get size
     *
     * @return size
     */
    public int size() {
        return size;
    }

    private void grow() {
        elements = Arrays.copyOf(elements, nextCapacity(elements.length));
    }

    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            int remaining = size;
            final int[] array = elements;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public Integer next() {
                return array[--remaining];
            }

        };
    }

    @Override
    public Spliterator<Integer> spliterator() {
        return Spliterators.spliterator(iterator(), size, Spliterator.SIZED | Spliterator.ORDERED);
    }
}
