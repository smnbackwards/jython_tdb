package org.python.modules._odb;

import org.python.core.Py;
import org.python.core.PyObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nms12 on 4/22/2016.
 */
public class HistoryMap<K>{

    protected Map<K, LocalValueList > map = new HashMap<>();

    public int size(int timestamp) {
        return map.size();
    }

    public boolean isEmpty(int timestamp) {
        return map.isEmpty();
    }

    public PyObject get(int timestamp, Object key) {
        return map.get(key).getValue(timestamp);
    }

    public PyObject put(int timestamp, K key, PyObject value) {
        if(map.containsKey(key)){
            return map.get(key).insertValue(timestamp, value);
        } else {
            map.put(key, new LocalValueList(timestamp, value));
            return null;
        }
    }

    public List<LocalValue> getBefore(int timestamp, Object key) {
        LocalValueList valueList = map.get(key);
        if(valueList == null){
            return null;
        }
        return valueList.values.stream().filter(localValue -> localValue.timestamp <= timestamp).collect(Collectors.toList());
    }

    public PyObject remove(Object key) {
        return null;
    }

    public void putAll(Map<? extends K, PyObject> m) {
        throw new NotImplementedException();
    }

    public void clear() {
        throw new NotImplementedException();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Collection<PyObject> values() {
        throw new NotImplementedException();
    }

}
