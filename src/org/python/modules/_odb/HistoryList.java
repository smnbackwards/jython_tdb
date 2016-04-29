package org.python.modules._odb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by nms12 on 4/28/2016.
 */
public class HistoryList<V> {
    protected List<HistoryValueList<V>> list = new ArrayList<>();
    protected HistoryValueList<Integer> size = new HistoryValueList<>(-1, 0);

    public int size(int timestamp) {
        return size.getValue(timestamp);
    }

    public V get(int timestamp, int index) {
        return list.get(index).getValue(timestamp);
    }

    public void add(int timestamp, V value) {
        if (list.size() < size.peekValue()) {
            list.get(list.size() + 1).insertValue(timestamp, value);
        } else {
            list.add(new HistoryValueList<V>(timestamp, value));
        }
        size.insertValue(timestamp, size.peekValue() + 1);
    }

    public void add(int timestamp, int index, V value) {
        int currentSize = size.peekValue();
        if (index >= 0 && index <= currentSize) {
            //Shuffle down the old values
            for (int i = index + 1; i < currentSize; i++) {
                list.get(i).insertValue(timestamp, list.get(i - 1).getValue(timestamp-1));
            }
            //shuffle the last value to a new entry
            if(currentSize > 0) {
                add(timestamp, list.get(currentSize - 1).getValue(timestamp-1));
            }

            //add the new value
            if(index < list.size()){
                list.get(index).insertValue(timestamp, value);
            } else {
                list.add(new HistoryValueList<V>(timestamp, value));
            }
            //update the size (use currentSize, since add(..) updates size as well)
            size.insertValue(timestamp, currentSize+1);
        }
    }

    public Iterator<V> getIterator(int timestamp) {
        return new Itr(timestamp);
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
}
