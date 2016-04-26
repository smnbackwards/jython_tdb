package org.python.modules._odb;

import org.python.core.PyObject;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedMethod;
import org.python.expose.ExposedType;

@ExposedType(name = "odb.event")
public class OdbEvent extends PyObject {

    public enum Type { LINE, CALL, RETURN }

    @ExposedGet
    public int timestamp;
    @ExposedGet
    public int lineno;
    @ExposedGet
    public OdbFrame frame;
    public Type eventType;

    public OdbEvent(int timestamp, int lineno, OdbFrame frame, Type eventType) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.lineno = lineno;
        this.frame = frame;
    }

    @Override
    public String toString() {
        if (frame == null){
            return String.format("<%s> \t%s \t%s:%s", timestamp, eventType.toString(), "<top level>", lineno);
        }
        return String.format("<%s> \t%s \t%s:%s", timestamp, eventType.toString(), frame.filename, lineno);
    }

    @ExposedMethod(names = "is_line")
    public boolean isLine(){
        return eventType == Type.LINE;
    }

    @ExposedMethod(names = "is_call")
    public boolean isCall(){
        return eventType == Type.CALL;
    }

    @ExposedMethod(names = "is_return")
    public boolean isReturn(){
        return eventType == Type.RETURN;
    }

}