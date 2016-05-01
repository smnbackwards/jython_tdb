package org.python.modules._odb;

import org.python.core.Py;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Created by nms12 on 4/29/2016.
 * A list implementation tied to the ODB omniscient debugger module
 * When recording is enabled, actions performed on this list are
 */
public class OdbList<V> implements List<V>, RandomAccess {
    private List<V> list;
    private HistoryList<V> historyList;

    public OdbList(){
        list = new ArrayList<>();
        historyList = new HistoryList<>();
    }

    public OdbList(Collection<? extends V> c){
        list = new ArrayList<>(c);
        historyList = new HistoryList<>();
    }

    protected boolean isReplaying(){
        return _odb.replaying;// && historyList.hasHistory();
    }

    protected boolean isRecording(){
        return true;//TODO is this the paradigm we want to use? _odb.enabled;
    }

    protected int getTimestamp(){
        return _odb.getCurrentTimestamp();
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public int size() {
        if(isReplaying()){
            return historyList.size(getTimestamp());
        }
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        if(isReplaying()){
            return historyList.size(getTimestamp()) == 0;
        }
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if(isReplaying()){
            return historyList.contains(getTimestamp(), o);
        }
        return list.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        if(isReplaying()){
            return historyList.iterator(getTimestamp());
        }
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        if(isReplaying()){
            return historyList.toArray(getTimestamp());
        }
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if(isReplaying()){
            //TODO
            throw new UnsupportedOperationException("toArray(T[]) in replay mode");
        }
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
            historyList.remove(getTimestamp(), o);
        }
        return list.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        if(isReplaying()){
            return historyList.containsAll(getTimestamp(), c);
        }
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        if(isRecording()){
            historyList.addAll(getTimestamp(), c);
        }
        return list.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends V> c) {
        if(isRecording()){
            historyList.addAll(getTimestamp(), index, c);
        }
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = list.removeAll(c);
        if(isRecording()){
            historyList.update(getTimestamp(), list);
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean modified = list.retainAll(c);
        if(isRecording()){
            historyList.update(getTimestamp(), list);
        }
        return modified;
    }

    @Override
    public void replaceAll(UnaryOperator<V> operator) {
        list.replaceAll(operator);
        if(isRecording()){
            historyList.update(getTimestamp(), list);
        }
    }

    @Override
    public void sort(Comparator<? super V> c) {
        list.sort(c);
        if(isRecording()){
            historyList.update(getTimestamp(), list);
        }
    }

    @Override
    public void clear() {
        if(isRecording()){
            historyList.clear(getTimestamp());
        }
        list.clear();
    }

    @Override
    public V get(int index) {
        if(isReplaying()){
            return historyList.get(getTimestamp(), index);
        }
        return list.get(index);
    }

    @Override
    public V set(int index, V element) {
        if(isRecording()){
            historyList.set(getTimestamp(), index, element);
        }
        return list.set(index, element);
    }

    @Override
    public void add(int index, V element) {
        if(isRecording()){
            historyList.add(getTimestamp(), index, element);
        }
        list.add(index, element);
    }

    @Override
    public V remove(int index) {
        if(isRecording()){
            historyList.remove(getTimestamp(), index);
        }
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        if(isReplaying()){
            return historyList.indexOf(getTimestamp(), o);
        }
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        if(isReplaying()){
            return historyList.lastIndexOf(getTimestamp(), o);
        }
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<V> listIterator() {
        if(isReplaying()){
            return historyList.listIterator(getTimestamp());
        }
        return list.listIterator();
    }

    @Override
    public ListIterator<V> listIterator(int index) {
        if(isReplaying()){
            return historyList.listIterator(getTimestamp(), index);
        }
        return list.listIterator(index);
    }

    @Override
    public List<V> subList(int fromIndex, int toIndex) {
        //TODO
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<V> spliterator() {
        //TODO
        return list.spliterator();
    }
}
