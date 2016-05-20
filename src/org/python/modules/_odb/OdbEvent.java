package org.python.modules._odb;

public abstract class OdbEvent {

    public int lineno;
    public OdbFrame frame;

    public OdbEvent(int lineno, OdbFrame frame) {
        this.lineno = lineno;
        this.frame = frame;
    }

    @Override
    public String toString() {
        return String.format("<%s> \t%s \t%s:%s", "%s", event_type(), frame.filename, lineno);
    }

    public abstract String event_type();

    public enum EVENT_TYPE {LINE, CALL, RETURN, EXCEPTION}
    protected static long createEvent(int lineno, int frameid, EVENT_TYPE type){
        return (((long)type.ordinal()) << 62 ) | (((long)lineno) << 32) | (frameid & 0xffffffffL);
    }

    protected static EVENT_TYPE decodeEventType(long event){
        return EVENT_TYPE.values()[(int)(event>>>62)]; // >>> is unsigned shift since we don't care about the negative
    }

    protected static int decodeEventLineno(long event){
        return ((int)(event>>32)) & 0x3fffffff; // hex for 0011 1111 ... 1111
    }

    protected static int decodeEventFrameId(long event){
        return (int)event;
    }

}