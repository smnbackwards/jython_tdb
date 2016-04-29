package org.python.modules._odb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * Created by nms12 on 4/27/2016.
 */
public class HistoryValueList<V> {

    Stack<HistoryValue<V>> values = new Stack<>();

    public HistoryValueList(int timestamp, V value) {
        values.push(new HistoryValue<V>(timestamp, value));
    }

    public V insertValue(int timestamp, V value) {
        //TODO return old value if timestamp is overriding?
        HistoryValue<V> topValue = values.peek();
        if (!topValue.getValue().equals(value)) {
            //Replace values with the same timestamp
            if(topValue.getTimestamp() == timestamp){
                values.pop();
            }
            //Only insert new values to save space
            values.push(new HistoryValue<V>(timestamp, value));
        }
        return null;
    }

    public V getValue(int timestamp) {
        V value = null;
        for (Iterator<HistoryValue<V>> it = values.iterator(); it.hasNext(); ) {
            HistoryValue<V> tempValue = it.next();
            if (tempValue.timestamp <= timestamp){
                value = tempValue.value;
            } else {
                break;
            }
        }
        return value;
    }

    public V peekValue() {
        return values.peek().getValue();
    }
}
