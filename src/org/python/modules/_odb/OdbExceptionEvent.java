package org.python.modules._odb;

import org.python.core.PyObject;

/**
 * Created by nms12 on 5/13/2016.
 */
public class OdbExceptionEvent extends OdbEvent {
    public final PyObject type;
    public final PyObject value;
    public final PyObject traceback;

    public OdbExceptionEvent(int timestamp, int lineno, OdbFrame frame, PyObject type, PyObject value, PyObject traceback) {
        super(timestamp, lineno, frame);
        this.type = type;
        this.value = value;
        this.traceback = traceback;
    }

    @Override
    public String event_type() {
        return "Exception";
    }
}
