package org.python.modules._odb;

import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "odb.event")
public class OdbEvent extends PyObject {

    public enum Type { LINE, CALL, RETURN }

    public long timestamp;
    public int lineNumber;
    public OdbFrame frame;
    public Type eventType;

    public OdbEvent(long timestamp, int lineNumber, OdbFrame frame, Type eventType) {
        this.timestamp = timestamp;
        this.eventType = eventType;
        this.lineNumber = lineNumber;
        this.frame = frame;
    }

    @Override
    public String toString() {
        if (frame == null){
            return String.format("<%s> %s %s:%s", timestamp, eventType.toString(), "<top level>", lineNumber);
        }
        return String.format("<%s> %s %s:%s", timestamp, eventType.toString(), frame.filename, lineNumber);
    }

}