package org.python.modules._odb;

import org.python.core.Py;
import org.python.core.PyObject;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by nms12 on 4/22/2016.
 */
public class HistoryMap<K,V>{

    class LocalValue {
        int timestamp;
        V value;

        public LocalValue(int timestamp, V value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public V getValue(int timestamp) {
            if (timestamp >= this.timestamp) {
                return this.value;
            } else {
                return null;
            }
        }
    }

    class LocalValueList {

        Stack<LocalValue> values = new Stack<>();

        public LocalValueList(int timestamp, V value) {
            insertValue(timestamp, value);
        }

        public V insertValue(int timestamp, V value) {
            //TODO return old value if timestamp is overriding?
            Py.writeMessage("TTD", String.format("inserting %s at %s", value, timestamp));
            values.push(new LocalValue(timestamp, value));
            return null;
        }

        public V getValue(int timestamp) {
            for(int i = values.size()-1; i >= 0; i--){
                if( values.get(i).timestamp < timestamp )
                    return values.get(i).value;
            }
            return null;
        }
    }

    protected Map<K, LocalValueList > map = new HashMap<>();

    public int size(int timestamp) {
        return map.size();
    }

    public boolean isEmpty(int timestamp) {
        return map.isEmpty();
    }

    public V get(int timestamp, Object key) {
        return map.get(key).getValue(timestamp);
    }

    public V put(int timestamp, K key, V value) {
        if(map.containsKey(key)){
            return map.get(key).insertValue(timestamp, value);
        } else {
            map.put(key, new LocalValueList(timestamp, value));
            return null;
        }
    }

    public V remove(Object key) {
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        throw new NotImplementedException();
    }

    public void clear() {
        throw new NotImplementedException();
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Collection<V> values() {
        throw new NotImplementedException();
    }

}
