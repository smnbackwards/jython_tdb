package org.python.modules._odb;

import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;


@ExposedType(name = "_odb.odbframe")
public class OdbFrame extends PyObject {

    public static final PyType TYPE = PyType.fromClass(OdbFrame.class);

    @ExposedGet
    public OdbFrame parent;
    @ExposedGet(name = "name")
    public String methodName;
    @ExposedGet
    public long timestamp;
    @ExposedGet
    public String filename;
    @ExposedGet
    public int lineno;


    public OdbFrame(long timestamp, String filename, int lineno, String methodName, OdbFrame parent) {
        super(TYPE);
        this.timestamp = timestamp;
        this.filename = filename;
        this.lineno = lineno;
        this.methodName = methodName;
        this.parent = parent;
    }
}
