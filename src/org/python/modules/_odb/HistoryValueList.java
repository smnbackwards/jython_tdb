package org.python.modules._odb;

import java.util.*;

/**
 * Created by nms12 on 4/27/2016.
 */
public class HistoryValueList<V> {

    ArrayDeque<HistoryValue<V>> values = new ArrayDeque<>();

    public HistoryValueList(int timestamp, V value) {
        values.addLast(new HistoryValue<V>(timestamp, value));
    }

    public V insertValue(int timestamp, V value) {
        //TODO return old value if timestamp is overriding?
        HistoryValue<V> topValue = values.peekLast();
        if (!Objects.equals(topValue.getValue(),value)) {
            //Replace values with the same timestamp
            if(topValue.getTimestamp() == timestamp){
                values.removeLast();
            }
            //Only insert new values to save space
            values.addLast(new HistoryValue<V>(timestamp, value));
        }
        return null;
    }

    public HistoryValue<V> getHistoryValue(int timestamp){
        HistoryValue<V> value = null;
        for (Iterator<HistoryValue<V>> it = values.iterator(); it.hasNext(); ) {
            HistoryValue<V> tempValue = it.next();
            if(tempValue.timestamp == timestamp){
                return tempValue;
            }
            if (tempValue.timestamp < timestamp){
                value = tempValue;
            } else {
                break;
            }
        }
        return value == null ? null : value;
    }

    public V getValue(int timestamp) {
        HistoryValue<V> value = null;
        for (Iterator<HistoryValue<V>> it = values.iterator(); it.hasNext(); ) {
            HistoryValue<V> tempValue = it.next();
            if(tempValue.timestamp == timestamp){
                return tempValue.value;
            }
            if (tempValue.timestamp < timestamp){
                value = tempValue;
            } else {
                break;
            }
        }
        return value == null ? null : value.getValue();
    }

    public V peekValue() {
        return values.peekLast().getValue();
    }
}
