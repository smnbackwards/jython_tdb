package org.python.modules._odb;

import org.python.core.PyObject;

import java.util.Stack;

/**
 * Created by nms12 on 4/25/2016.
 */
class LocalValueList extends HistoryValueList<PyObject> {

    public LocalValueList(int timestamp, PyObject value) {
        super(timestamp, value);
    }

}