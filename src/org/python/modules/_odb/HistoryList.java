package org.python.modules._odb;

import java.util.*;

/**
 * Created by nms12 on 4/28/2016.
 */
public class HistoryList<V> {
    protected List<HistoryValueList<V>> list = new ArrayList<>();
    protected HistoryValueList<Integer> size = new HistoryValueList<>(-1, 0);

    public boolean hasHistory() {
        return size.values.size() > 1;
    }

    private int rangeCheck(int timestamp, int index) {
        int size = size(timestamp);
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(timestamp, index));
        } else
            return size;
    }

    /**
     * A version of rangeCheck used by add and addAll.
     */
    private int rangeCheckForAdd(int timestamp, int index) {
        int size = size(timestamp);
        if (index > size || index < 0) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(timestamp, index));
        } else
            return size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int timestamp, int index) {
        return "Timestamp: " + timestamp + ", Index: " + index + ", Size: " + size(timestamp);
    }

    public int size(int timestamp) {
        return size.getValue(timestamp);
    }

    public boolean contains(int timestamp, Object o) {
        return indexOf(timestamp, o) >= 0;
    }

    public boolean containsAll(int timestamp, Collection<?> c) {
        return c.stream().allMatch(item -> contains(timestamp, item));
    }

    public Object[] toArray(int timestamp) {
        return list.stream().limit(size(timestamp)).map(l -> l.getValue(timestamp)).toArray();
    }

    public void clear(int timestamp) {
        size.insertValue(timestamp, 0);
    }

    public void removeRange(int timestamp, int fromIndex, int toIndex) {
        int size = size(timestamp);
        if (fromIndex == 0 && toIndex == size) {
            clear(timestamp);
        } else {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException();
            }

            int numMoved = size - toIndex;
            for (int i = 0; i < numMoved; i++) {
                set(timestamp, fromIndex + i, get(timestamp, toIndex + i));
            }

            this.size.insertValue(timestamp, size - (toIndex - fromIndex));
        }
    }

    public V get(int timestamp, int index) {
        if (index < 0 || index >= size(timestamp)) {
            throw new IndexOutOfBoundsException();
        }
        return list.get(index).getValue(timestamp);
    }

    public void add(int timestamp, V value) {
        int currentSize = size.getValue(timestamp);
        if (currentSize < list.size()) {
            //The backing list has more elements than the reported size
            //So just store in the existing element
            list.get(currentSize).insertValue(timestamp, value);
        } else {
            list.add(new HistoryValueList<V>(timestamp, value));
        }
        size.insertValue(timestamp, currentSize + 1);
    }

    public void add(int timestamp, int index, V value) {
        int currentSize = rangeCheckForAdd(timestamp, index);
        //Shuffle down the old values
        int past = timestamp == 0 ? 0 : timestamp - 1;
        for (int i = index + 1; i < currentSize; i++) {
            list.get(i).insertValue(timestamp, list.get(i - 1).getValue(past));
        }
        //shuffle the last value to a new entry
        if (currentSize > 0) {
            HistoryValueList<V> valueList = list.get(currentSize - 1);
            add(timestamp, valueList.getValue(past));
        }

        //add the new value
        if (index < list.size()) {
            list.get(index).insertValue(timestamp, value);
        } else {
            list.add(new HistoryValueList<V>(timestamp, value));
        }
        //update the size (use currentSize, since add(..) updates size as well)
        size.insertValue(timestamp, currentSize + 1);
    }

    public V set(int timestamp, int index, V element) {
        rangeCheck(timestamp - 1, index);
        list.get(index).insertValue(timestamp, element);
        return list.get(index).getValue(timestamp - 1);
    }

    public V remove(int timestamp, int index) {
        int currentSize = rangeCheck(timestamp, index);
        size.insertValue(timestamp, currentSize - 1);
        V value = list.get(index).peekValue();
        for (int i = index; i < currentSize - 1; i++) {
            list.get(i).insertValue(timestamp, list.get(i + 1).peekValue());
        }
        return value;
    }

    public int indexOf(int timestamp, Object o) {
        if (o == null) {
            for (int i = 0; i < size(timestamp); i++)
                if (list.get(i).getValue(timestamp) == null)
                    return i;
        } else {
            for (int i = 0; i < size(timestamp); i++)
                if (o.equals(list.get(i).getValue(timestamp)))
                    return i;
        }
        return -1;
    }

    public int lastIndexOf(int timestamp, Object o) {
        if (o == null) {
            for (int i = size(timestamp) - 1; i >= 0; i--)
                if (list.get(i).getValue(timestamp) == null)
                    return i;
        } else {
            for (int i = size(timestamp) - 1; i >= 0; i--)
                if (o.equals(list.get(i).getValue(i)))
                    return i;
        }
        return -1;
    }

    public boolean remove(int timestamp, Object o) {
        int index = indexOf(timestamp, o);
        if (index >= 0) {
            remove(timestamp, index);
        }
        return false;
    }

    public Iterator<V> iterator(int timestamp) {
        return new Itr(timestamp);
    }

    public ListIterator<V> listIterator(int timestamp) {
        return listIterator(timestamp, 0);
    }

    public ListIterator<V> listIterator(int timestamp, int index) {
        return new ListItr(timestamp, index);
    }

    public boolean addAll(int timestamp, Collection<? extends V> c) {
        int currentSize = size(timestamp);
        int i = 0;
        for (Iterator<? extends V> itr = c.iterator(); itr.hasNext(); ) {
            setOrAdd(currentSize + (i++), timestamp, itr.next());
        }
        size.insertValue(timestamp, size.peekValue() + c.size());
        return c.size() != 0;
    }

    public boolean addAll(int timestamp, int index, Collection<? extends V> c) {
        int currentSize = size(timestamp);
        int offset = c.size();
        if (index >= 0 && index <= currentSize) {
            Iterator<? extends V> insertIterator = c.iterator();
            int pastTime = timestamp <= 0 ? 0 : timestamp - 1;
            //Shuffle down the old values
            for (int i = index; i < offset + currentSize; i++) {
                if (i < index + offset) {
                    V insertValue = insertIterator.next();
                    setOrAdd(i, timestamp, insertValue);
                } else {
                    V currentValue = list.get(i - offset).getValue(pastTime);
                    setOrAdd(i, timestamp, currentValue);
                }
            }

            size.insertValue(timestamp, currentSize + offset);
        }

        return false;
    }

    protected void setOrAdd(int index, int timestamp, V value) {
        if (index - list.size() > 1) {
            throw new IndexOutOfBoundsException("only supports increasing the list by 1 value at a time");
        }
        if (index >= list.size()) {
            list.add(new HistoryValueList<V>(timestamp, value));
        } else {
            list.get(index).insertValue(timestamp, value);
        }
    }

    public void update(int timestamp, List<V> referenceList) {
        Objects.requireNonNull(referenceList);
        if (referenceList.size() > size(timestamp)) {
            throw new UnsupportedOperationException("the reference list must be the same size or smaller");
        }

        for (ListIterator<V> it = referenceList.listIterator(); it.hasNext(); ) {
            int index = it.nextIndex();
            list.get(index).insertValue(timestamp, it.next());
        }
        this.size.insertValue(timestamp, referenceList.size());
    }

    private class Itr implements Iterator<V> {


        private final int timestamp;
        private int index = 0;
        private final Iterator<HistoryValueList<V>> it = HistoryList.this.list.iterator();

        public Itr(int timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext() && index != HistoryList.this.size(timestamp);
        }

        @Override
        public V next() {
            index = index + 1;
            HistoryValueList<V> l = it.next();
            V value = l.getValue(timestamp);
            return value;
        }
    }

    private class ListItr implements ListIterator<V> {

        private final int timestamp;
        private int index;
        private final ListIterator<HistoryValueList<V>> it;

        public ListItr(int timestamp, int index) {
            this.timestamp = timestamp;
            this.index = index;
            it = HistoryList.this.list.listIterator(index);
        }

        @Override
        public boolean hasNext() {
            return it.hasNext() && index < HistoryList.this.size(timestamp);
        }

        @Override
        public V next() {
            index++;
            return it.next().getValue(timestamp);
        }

        @Override
        public boolean hasPrevious() {
            return it.hasPrevious();
        }

        @Override
        public V previous() {
            index--;
            return it.previous().getValue(timestamp);
        }

        @Override
        public int nextIndex() {
            return it.nextIndex();
        }

        @Override
        public int previousIndex() {
            return it.previousIndex();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void set(V v) {
            throw new UnsupportedOperationException("set");
        }

        @Override
        public void add(V v) {
            throw new UnsupportedOperationException("add");
        }
    }
}
