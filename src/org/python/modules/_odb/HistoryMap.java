package org.python.modules._odb;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nms12 on 4/22/2016.
 */
public class HistoryMap<K, V> {

    protected Map<K, HistoryValueList<V>> map = new HashMap<>();
    protected HistoryValueList<Integer> size = new HistoryValueList<>(-1, 0);

    protected void changeSizeBy(int timestamp, int amount){
        HistoryValue<Integer> value = size.getHistoryValue(timestamp);
        if(value != null && value.getTimestamp() == timestamp){
            value.value += amount;
        } else {
            size.insertValue(timestamp, value.value + amount);
        }
    }

    public int size(int timestamp) {
        return size.getValue(timestamp);
    }

    public boolean isEmpty(int timestamp) {
        return size(timestamp) == 0;
    }

    public V get(int timestamp, Object key) {
        HistoryValueList<V> list = map.get(key);
        return list == null ? null : list.getValue(timestamp);
    }

    public V put(int timestamp, K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        if (map.containsKey(key)) {
            return map.get(key).insertValue(timestamp, value);
        } else {
            map.put(key, new HistoryValueList<V>(timestamp, value));
            changeSizeBy(timestamp, 1);
            return null;
        }
    }

    public List<HistoryValue<V>> getBefore(int timestamp, Object key) {
        HistoryValueList<V> valueList = map.get(key);
        if (valueList == null) {
            return null;
        }
        return valueList.values.stream().filter(localValue -> localValue.timestamp <= timestamp).collect(Collectors.toList());
    }

    public void putAll(int timestamp, Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(timestamp, e.getKey(), e.getValue());
    }

    public void clear(int timestamp) {
        for (HistoryValueList<V> list : map.values()) {
            list.insertValue(timestamp, null);
        }
        size.insertValue(timestamp, 0);
    }

    public boolean remove(int timestamp, Object key, Object value) {
        return Objects.equals(remove(timestamp, key),value);
    }

    public V remove(int timestamp, Object key) {
        if (key == null) {
            return null;
        }

        HistoryValueList<V> list = map.get(key);
        if (list != null) {
            V value = list.getValue(timestamp);
            list.insertValue(timestamp, null);
            changeSizeBy(timestamp, -1);
            return value;
        } else {
            return null;
        }
    }

    public boolean containsKey(int timestamp, Object key){
        HistoryValueList<V> list = map.get(key);
        return list == null ? false : list.getValue(timestamp) != null;
    }
}
