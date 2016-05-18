package org.python.modules._odb;

public abstract class OdbEvent {

    public int timestamp;
    public int lineno;
    public OdbFrame frame;

    public OdbEvent(int timestamp, int lineno, OdbFrame frame) {
        this.timestamp = timestamp;
        this.lineno = lineno;
        this.frame = frame;
    }

    @Override
    public String toString() {
        return String.format("<%s> \t%s \t%s:%s", timestamp, event_type(), frame.filename, lineno);
    }

    public abstract String event_type();

}