package org.python.modules._odb;

public class OdbEvent {

    public enum EVENT_TYPE {LINE, CALL, RETURN, EXCEPTION}
    public static long createEvent(int lineno, int frameid, EVENT_TYPE type){
        return (((long)type.ordinal()) << 62 ) | (((long)lineno) << 32) | (frameid & 0xffffffffL);
    }

    public static EVENT_TYPE decodeEventType(long event){
        return EVENT_TYPE.values()[(int)(event>>>62)]; // >>> is unsigned shift since we don't care about the negative
    }

    public static int decodeEventLineno(long event){
        return ((int)(event>>32)) & 0x3fffffff; // hex for 0011 1111 ... 1111
    }

    public static int decodeEventFrameId(long event){
        return (int)event;
    }

}