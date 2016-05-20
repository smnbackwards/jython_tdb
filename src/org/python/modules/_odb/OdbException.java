package org.python.modules._odb;

import org.python.core.PyObject;

/**
 * Created by nms12 on 5/20/2016.
 */
public class OdbException {

    public final int timestamp;
    public final PyObject type;
    public final PyObject value;
    public final PyObject traceback;

    public OdbException(int timestamp, PyObject type, PyObject value, PyObject traceback) {
        this.timestamp = timestamp;
        this.type = type;
        this.value = value;
        this.traceback = traceback;
    }
}
