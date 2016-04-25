package org.python.modules._odb;

import org.python.core.PyObject;

import java.util.Stack;

/**
 * Created by nms12 on 4/25/2016.
 */
class LocalValueList {

    Stack<LocalValue> values = new Stack<>();

    public LocalValueList(int timestamp, PyObject value) {
        insertValue(timestamp, value);
    }

    public PyObject insertValue(int timestamp, PyObject value) {
        //TODO return old value if timestamp is overriding?
        values.push(new LocalValue(timestamp, value));
        return null;
    }

    public PyObject getValue(int timestamp) {
        for(int i = values.size()-1; i >= 0; i--){
            if( values.get(i).timestamp <= timestamp )
                return values.get(i).value;
        }
        return null;
    }
}