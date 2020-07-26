package cc.aguesuka.maguet.util;

import java.util.*;

/**
 * An int stack implements by an int array
 *
 * @author aguesuka
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
     * Adds at last
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
     * Remove and returns the last element
     *
     * @return last element
     */
    public int pop() {
        if (size <= 0) {
            throw new NoSuchElementException();
        }
        size--;
        return elements[size];
    }

    /**
     * Removes all value
     */
    public void clear() {
        size = 0;
    }

    /**
     * Returns the elements count
     *
     * @return elements count
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
            final int[] array = elements;
            int remaining = size;

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
