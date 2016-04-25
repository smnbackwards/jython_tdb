package org.python.modules._odb;

import org.python.core.PyObject;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Created by nms12 on 4/23/2016.
 */
@ExposedType(name = "odb.LocalValue")
public class LocalValue {
    int timestamp;
    PyObject value;

    public LocalValue(int timestamp, PyObject value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("<%s> : %s", timestamp, value);
    }

    @ExposedGet(name = "timestamp")
    public int getTimestamp() {
        return timestamp;
    }

    @ExposedGet(name = "value")
    public PyObject getValue() {
        return value;
    }

    @ExposedMethod(names = "get_value")
    public PyObject getValue(int timestamp) {
        if (timestamp >= this.timestamp) {
            return this.value;
        } else {
            return null;
        }
    }
}
