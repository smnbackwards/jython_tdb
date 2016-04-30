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

    public int size(int timestamp) {
        return size.getValue(timestamp);
    }

    public boolean contains(int timestamp, Object o) {
        return indexOf(timestamp, o) > 0;
    }

    public boolean containsAll(int timestamp, Collection<?> c) {
        return c.stream().allMatch(item -> contains(timestamp, item));
    }

    public Object[] toArray(int timestamp) {
        return list.stream().limit(size(timestamp)).map(l -> l.getValue(timestamp)).toArray();
    }

    public V get(int timestamp, int index) {
        return list.get(index).getValue(timestamp);
    }

    public void add(int timestamp, V value) {
        int currentSize =  size.getValue(timestamp);
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
        int currentSize = size.peekValue();
        if (index >= 0 && index <= currentSize) {
            //Shuffle down the old values
            for (int i = index + 1; i < currentSize; i++) {
                list.get(i).insertValue(timestamp, list.get(i - 1).getValue(timestamp - 1));
            }
            //shuffle the last value to a new entry
            if (currentSize > 0) {
                add(timestamp, list.get(currentSize - 1).getValue(timestamp - 1));
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
    }

    public V set(int timestamp, int index, V element) {
        int currentSize = size.peekValue();
        if (index >= 0 && index <= currentSize) {
            list.get(index).insertValue(timestamp, element);
            return list.get(index).getValue(timestamp - 1);
        }
        return null;
    }

    public V remove(int timestamp, int index) {
        int currentSize = size.peekValue();
        if (index >= 0 && index < currentSize) {
            size.insertValue(timestamp, currentSize - 1);
            V value = list.get(index).peekValue();
            for (int i = index; i < currentSize - 1; i++) {
                list.get(i).insertValue(timestamp, list.get(i + 1).peekValue());
            }
            return value;
        }

        return null;
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

    public Iterator<V> getIterator(int timestamp) {
        return new Itr(timestamp);
    }

    public ListIterator<V> listIterator(int timestamp) {
        return listIterator(timestamp, 0);
    }

    public ListIterator<V> listIterator(int timestamp, int index) {
        return new ListItr(timestamp, index);
    }

    public boolean addAll(int timestamp, Collection<? extends V> c) {
        c.forEach(v -> list.add(new HistoryValueList<V>(timestamp, v)));
        size.insertValue(timestamp, size.peekValue() + c.size());
        return c.size() != 0;
    }

    public boolean addAll(int timestamp, int index, Collection<? extends V> c) {
        int currentSize = size.peekValue();
        int offset = c.size();
        if (index >= 0 && index <= currentSize) {
            Iterator<? extends V> insertIterator = c.iterator();
            //Shuffle down the old values
            for (int i = 0; i < offset; i++) {
                int insertIndex = i + index;
                V insertValue = insertIterator.next();

                int shuffleIndex = insertIndex + offset;
                V currentValue = insertIndex < list.size() ?
                        list.get(insertIndex).getValue(timestamp-1)
                        : null;

                setOrAddListValue(insertIndex, timestamp, insertValue);

                if(currentValue != null) {
                    setOrAddListValue(shuffleIndex, timestamp, currentValue);
                }
            }

            size.insertValue(timestamp, size.peekValue() + offset);
        }

        return false;
    }

    protected void setOrAddListValue(int index, int timestamp, V value){
        if(index - list.size() > 1){
            throw new IndexOutOfBoundsException("only supports increasing the list by 1 value at a time");
        }
        if(index >= list.size()){
            list.add(new HistoryValueList<V>(timestamp, value));
        } else {
            list.get(index).insertValue(timestamp, value);
        }
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
