package org.python.modules._odb;

import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

import java.util.Map;


public class OdbFrame {

    //Use python style naming since we don't want to incur the memory overhead of exposing the type
    public final int index;
    public OdbFrame parent;
    public String name;
    public int timestamp;
    public int return_timestamp;
    public PyObject return_value;
    public String filename;
    public int lineno;
    public HistoryMap<Object, PyObject> locals;


    public OdbFrame(int index, int timestamp, String filename, int lineno, String methodName, OdbFrame parent, HistoryMap<Object, PyObject> locals) {
        this.index = index;
        this.timestamp = timestamp;
        this.filename = filename;
        this.lineno = lineno;
        this.name = methodName;
        this.parent = parent;
        this.locals = locals;
    }

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
