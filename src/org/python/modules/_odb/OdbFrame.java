package org.python.modules._odb;

import org.python.core.*;
import org.python.expose.ExposedGet;
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
    @ExposedGet
    public int returnTimestamp;
    @ExposedGet
    public PyObject returnValue;
    @ExposedGet
    public String filename;
    @ExposedGet
    public int lineno;
    public HistoryMap<Object> locals;


    public OdbFrame(int timestamp, String filename, int lineno, String methodName, OdbFrame parent, HistoryMap<Object> locals) {
        super(TYPE);
        this.timestamp = timestamp;
        this.filename = filename;
        this.lineno = lineno;
        this.methodName = methodName;
        this.parent = parent;
        this.locals = locals;
    }

    public PyStringMap getLocals(int timestamp){
        PyStringMap map = new PyStringMap();
        locals.map.keySet().stream().forEach(o -> map.__setitem__((String)o, locals.get(timestamp, o)));
        return map;
    }
}
