package org.python.modules._odb;

import org.python.core.PyObject;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

/**
 * Created by nms12 on 4/23/2016.
 */
@ExposedType(name = "odb.LocalValue")
public class LocalValue extends HistoryValue<PyObject> {

    public LocalValue(int timestamp, PyObject value) {
        super(timestamp, value);
    }
}
