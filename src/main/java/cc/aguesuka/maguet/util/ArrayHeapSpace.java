package cc.aguesuka.maguet.util;

import java.util.*;

/**
 * A data structure, this can {@link #add(E element) add} return the index, and {@link #remove(int, E) remove}  by
 * index. element is nonnull. keep elements not equals each other.
 *
 * @param <E> element type
 * @author aguesuka
 */
public final class ArrayHeapSpace<E> implements Iterable<E> {
    final private ArrayList<E> data;
    final private IntArrayStack readyIndexStack;
    private int size = 0;

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
