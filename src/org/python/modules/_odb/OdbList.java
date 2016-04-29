package org.python.modules._odb;

import java.util.*;

/**
 * Created by nms12 on 4/29/2016.
 */
public class OdbList<V> implements List<V> {
    private List<V> list;
    private HistoryList<V> historyList;

    protected boolean isReplaying(){
        return _odb.replaying;
    }

    protected boolean isRecording(){
        return _odb.enabled;
    }

    protected int getTimestamp(){
        return _odb.getCurrentTimestamp();
    }


    @Override
    public int size() {
        if(isReplaying()){
            return historyList.size(getTimestamp());
        } else {
            return list.size();
        }
    }

    @Override
    public boolean isEmpty() {
        if(isReplaying()){
            return historyList.size(getTimestamp()) == 0;
        } else {
            return list.isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public Iterator<V> iterator() {
        if(isReplaying()){
            return historyList.getIterator(getTimestamp());
        } else {
            return list.iterator();
        }
    }

    @Override
    public Object[] toArray() {
        //TODO
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        //TODO
        return list.toArray(a);
    }

    @Override
    public boolean add(V v) {
        if(isRecording()){
            historyList.add(getTimestamp(), v);
        }
        return list.add(v);
    }

    @Override
    public boolean remove(Object o) {
        if(isRecording()){
//            historyList.remove(getTimestamp(), o);
        }
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if(isRecording()){
//            historyList.add(getTimestamp(), v);
        }
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        return list.addAll(index,c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public V get(int index) {
        if(isReplaying()){
            return historyList.get(getTimestamp(), index);
        } else {
            return list.get(index);
        }
    }

    @Override
    public V set(int index, V element) {
        if(isRecording()){
//            historyList.add(getTimestamp(), v);
        }
        return list.set(index, element);
    }

    @Override
    public void add(int index, V element) {
        if(isRecording()) {
            historyList.add(getTimestamp(), index, element);
        }
        list.add(index, element);
    }

    @Override
    public V remove(int index) {
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<V> listIterator() {
        return list.listIterator();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        return list.listIterator(index);
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }
}
