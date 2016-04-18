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
    public String filename;
    @ExposedGet
    public int lineno;
    public Map<Object, PyObject> locals;


    public OdbFrame(int timestamp, String filename, int lineno, String methodName, OdbFrame parent, Map<Object,PyObject> locals) {
        super(TYPE);
        this.timestamp = timestamp;
        this.filename = filename;
        this.lineno = lineno;
        this.methodName = methodName;
        this.parent = parent;
        this.locals = locals;
    }

    @ExposedGet(name = "locals")
    public PyStringMap getLocals(){
        return new PyStringMap(locals);
    }
}
