package cc.aguesuka.btfind.util;

import java.util.*;

/**
 * a data struct {@link #add} return the index and can {@link #remove(int, E)}  by index.
 * element is null null. keep every element not equals other.
 *
 * @param <E> element type
 * @author :aguesuka
 * 2019/12/28 15:25
 */
public final class ArrayHeapSpace<E> implements Iterable<E> {
    private int size = 0;
    private ArrayList<E> data;
    private IntArrayStack readyIndexStack;

    public ArrayHeapSpace() {
        data = new ArrayList<>();
        readyIndexStack = new IntArrayStack();
    }

    public int size() {
        return size;
    }

    public int add(E element) {
        Objects.requireNonNull(element);
        size++;
        if (readyIndexStack.size() > 0) {
            int index = readyIndexStack.pop();
            data.set(index, element);
            return index;
        } else {
            data.add(element);
            return data.size() - 1;
        }
    }


    public boolean remove(int index, E element) {
        if (index >= data.size() || element != data.get(index)) {
            return false;
        }
        size--;
        data.set(index, null);

        assert size == data.stream().filter(Objects::nonNull).count();

        readyIndexStack.push(index);
        return true;
    }

    public void clear() {
        size = 0;
        data.clear();
        readyIndexStack.clear();
    }

    @Override
    public String toString() {
        return "ArrayHeapSpace size=" + size;
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(iterator(), size, Spliterator.SIZED);
    }

    @Override
    public Iterator<E> iterator() {
        return data.stream().filter(Objects::nonNull).iterator();
    }
}
