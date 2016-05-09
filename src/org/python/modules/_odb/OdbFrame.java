package org.python.modules._odb;

import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

import java.util.Map;


@ExposedType(name = "odb.frame")
public class OdbFrame extends PyObject {

    public static final PyType TYPE = PyType.fromClass(OdbFrame.class);

    @ExposedGet
    public OdbFrame parent;
    @ExposedGet(name = "name")
    public String methodName;
    @ExposedGet
    public int timestamp;
    @ExposedGet(name = "return_timestamp")
    public int returnTimestamp;
    @ExposedGet(name = "return_value")
    public PyObject returnValue;
    @ExposedGet
    public String filename;
    @ExposedGet
    public int lineno;
    public HistoryMap<Object, PyObject> locals;


    public OdbFrame(int timestamp, String filename, int lineno, String methodName, OdbFrame parent, HistoryMap<Object, PyObject> locals) {
        super(TYPE);
        this.timestamp = timestamp;
        this.filename = filename;
        this.lineno = lineno;
        this.methodName = methodName;
        this.parent = parent;
        this.locals = locals;
    }

    @ExposedMethod
    public PyStringMap getLocals(int timestamp){
        PyStringMap map = new PyStringMap();
        for( Map.Entry<Object, HistoryValueList<PyObject>> e : locals.map.entrySet()){
            HistoryValue<PyObject> value = e.getValue().getHistoryValue(timestamp);
            if(value != null && value.getValue() != null){
                map.__setitem__((String)e.getKey(), value.getValue());
            }
        }
        return map;
    }
}
