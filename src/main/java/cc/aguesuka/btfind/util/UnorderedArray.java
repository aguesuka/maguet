package cc.aguesuka.btfind.util;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * unordered array of add and remove cast O(1)
 *
 * @author :yangmingyuxing
 * 2019/12/28 15:25
 */
public final class UnorderedArray<E> {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private int size = 0;
    private Object[] data;
    private int[] empty;
    private int currentIndex = 0;
    private int emptyIndex = 0;

    public UnorderedArray(int initSize) {
        data = new Object[initSize];
        empty = EMPTY_INT_ARRAY;
    }

    public int size() {
        return size;
    }

    public int add(E element) {
        Objects.requireNonNull(element);

        if (size == data.length) {
            grow();
        }
        size++;
        int index = emptyIndex > 0 ? empty[--emptyIndex] : currentIndex++;
        data[index] = element;

        return index;
    }

    private void addEmpty(int index) {
        if (emptyIndex == empty.length) {
            int resize = Math.max(1, empty.length * 2);
            int[] newEmpty = new int[resize];
            System.arraycopy(empty, 0, newEmpty, 0, empty.length);
            empty = newEmpty;
        }
        empty[emptyIndex++] = index;
    }

    public void remove(int index, E element) {
        if (index >= data.length || element != data[index]) {
            return;
        }
        size--;
        data[index] = null;
        if (index == currentIndex - 1) {
            --currentIndex;
        } else {
            addEmpty(index);
        }
    }

    public void foreach(Consumer<E> action) {
        for (Object element : data) {
            @SuppressWarnings("unchecked")
            E e = (E) element;
            if (e != null) {
                action.accept(e);
            }
        }
    }

    public void clear() {
        size = 0;
        currentIndex = 0;
        emptyIndex = 0;
        Arrays.fill(data, null);
        empty = EMPTY_INT_ARRAY;
    }

    private void grow() {
        Object[] newData = new Object[data.length * 2];
        System.arraycopy(data, 0, newData, 0, data.length);
        data = newData;
    }

    @Override
    public String toString() {
        return this.getClass().toString()+" size:"  + size;
    }
}
