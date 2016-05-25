package org.python.modules._odb;

import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.helper.BigLists;
import org.magicwerk.brownies.collections.primitive.LongBigList;
import org.python.core.*;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {

    public static int getCurrentTimestamp(){
        return OdbTraceFunction.getCurrentTimestamp();
    }

    public static String getCurrentEventType(){
        return OdbTraceFunction.getCurrentEventType().toString();
    }

    public static int getCurrentEventLineno(){
        return OdbTraceFunction.getCurrentEventLineno();
    }

    public static int getCurrentFrameId(){
        return OdbTraceFunction.getCurrentFrameId();
    }

    public static OdbFrame getCurrentFrame(){
        return OdbTraceFunction.getCurrentFrame();
    }

    public static OdbException getCurrentException(){
        return OdbTraceFunction.getCurrentException();
    }

    public static void reset() {
        OdbTraceFunction.reset();
    }

    public static void setup() {
        OdbTraceFunction.setup();
    }

    public static String getEvent(int timestamp){
        LongBigList events = OdbTraceFunction.getEvents();
        if(timestamp >= 0 && timestamp < events.size() ) {
            long eventlong = OdbTraceFunction.getEvents().get(timestamp);
            int lineno = OdbEvent.decodeEventLineno(eventlong);
            int frameid = OdbEvent.decodeEventFrameId(eventlong);
            OdbFrame frame = getFrames().get(frameid);
            OdbEvent.EVENT_TYPE type = OdbEvent.decodeEventType(eventlong);
            return String.format("<%s> \t%s \t%s:%s\n", timestamp, type, frame.filename, lineno);
        }
        return null;
    }

    public static List<Integer> getLinenos(){
        LongBigList events = OdbTraceFunction.getEvents();
        List<Integer> linenos = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            linenos.add(OdbEvent.decodeEventLineno(events.get(i)));
        }
        return linenos;
    }

    public static List<String> getEventTypes(){
        LongBigList events = OdbTraceFunction.getEvents();
        List<String> types = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            types.add(OdbEvent.decodeEventType(events.get(i)).toString());
        }
        return types;
    }

    public static List<OdbFrame> getFrames() {
        return OdbTraceFunction.getFrames();
    }

    public static PyStringMap getCurrentLocals() {
        if (getCurrentFrame().locals == OdbTraceFunction.getGlobals()) {
            return getGlobals();
        }
        return OdbTraceFunction.getLocals();
    }

    public static PyStringMap getGlobals() {
        int currentTimestamp = getCurrentTimestamp();
        PyStringMap map = new PyStringMap();
        for (Map.Entry<Object, HistoryValueList<PyObject>> e : OdbTraceFunction.getGlobals().map.entrySet()) {
            HistoryValue<PyObject> value = e.getValue().getHistoryValue(currentTimestamp);
            if (value != null && value.getValue() != null && !e.getKey().equals("__builtins__")) {
                map.__setitem__((String) e.getKey(), value.getValue());
            }
        }
        return map;
    }

    public static List<HistoryValue<PyObject>> getLocalHistory(String key) {
        return getCurrentFrame().locals.getBefore(getCurrentTimestamp(), key);
    }

    public static PyStringMap getFrameArguments() {
        OdbFrame frame = getCurrentFrame();
        return frame.getLocals(frame.timestamp);
    }

    public static int do_step() {
        return do_jump(getCurrentTimestamp() + 1);
    }

    public static int do_rstep() {
        return do_jump(getCurrentTimestamp() - 1);
    }

    public static int do_return() {
        //if I am the return event I am looking for then just step 1
        if (OdbTraceFunction.getCurrentEventType() == OdbEvent.EVENT_TYPE.RETURN) {
            return do_step();
        }

        return do_jump(getCurrentFrame().return_timestamp);
    }

    public static int do_rreturn() {
        OdbFrame frame = getCurrentFrame();
        if (frame.timestamp == getCurrentTimestamp()) {
            return do_rstep();
        } else {
            return do_jump(frame.timestamp);
        }
    }

    public static int do_next() {
        OdbFrame frame = getCurrentFrame();

        if (OdbTraceFunction.getCurrentEventType() == OdbEvent.EVENT_TYPE.RETURN) {
            frame = frame.parent;
        }

        int frameid = frame == null ? 0 : frame.index;

        LongBigList events = OdbTraceFunction.getEvents();

        for (int i = getCurrentTimestamp() + 1; i < events.size(); i++) {
            long eventlong = events.get(i);
            if (OdbEvent.decodeEventFrameId(eventlong) == frameid) {
                return do_jump(i);
            }
        }
        return getCurrentTimestamp();
    }

    public static int do_rnext() {
        OdbFrame frame = getCurrentFrame();

        if (OdbTraceFunction.getCurrentEventType() == OdbEvent.EVENT_TYPE.CALL) {
            frame = frame.parent;
        }

        int frameid = frame.index;
        LongBigList events = OdbTraceFunction.getEvents();

        for (int i = getCurrentTimestamp() - 1; i >= 0; i--) {
            long eventlong = events.get(i);
            if (OdbEvent.decodeEventFrameId(eventlong) == frameid) {
                return do_jump(i);
            }
        }
        return getCurrentTimestamp();
    }


    public static int do_jump(int n) {
        OdbTraceFunction.moveToTimestamp(n);
        return getCurrentTimestamp();
    }

    public static int moveUpFrames() {
        OdbFrame frame = getCurrentFrame();
        if (frame == null) {
            return getCurrentTimestamp();
        }
        OdbTraceFunction.moveToFrame(frame.parent);
        return getCurrentTimestamp();
    }

    public static int moveDownFrames() {
        //We don't keep child links and the next frame is not guaranteed to be a child
        //Scan the events log to find a CALL event which has a frame with the current frame as its parent
        OdbFrame frame = getCurrentFrame();
        if (frame == null) {
            return getCurrentTimestamp();
        }
        LongBigList events = OdbTraceFunction.getEvents();
        Stack<OdbFrame> frames = OdbTraceFunction.getFrames();
        long eventlong = -1;
        int timestamp = -1;
        for (int i = getCurrentTimestamp(); i < events.size(); i++) {
            eventlong = events.get(i);
            OdbFrame eventframe = frames.get(OdbEvent.decodeEventFrameId(eventlong));
            if (eventframe != null && eventframe.parent == frame) {
                timestamp = i;
                break;
            }
        }

        if (timestamp >= 0) {
            OdbTraceFunction.moveToTimestamp(timestamp);
        }
        return getCurrentTimestamp();
    }

    public static int moveNextFrames() {
        OdbTraceFunction.moveToFrame(OdbTraceFunction.getCurrentFrameId() + 1);
        return getCurrentTimestamp();
    }

    public static int movePrevFrames() {
        OdbTraceFunction.moveToFrame(OdbTraceFunction.getCurrentFrameId() - 1);
        return getCurrentTimestamp();
    }

    public static void uncaughtExceptionEvent(PyBaseException exception){
        OdbTraceFunction.uncaughtExceptionEvent(exception);
    }

    //Static helper methods
    public static PyStringMap initializeGlobals(PyStringMap map) {
        //PyStringMap is not exposed to python in Jython
        //So we include this helper function to call the enableLogging method on it
        map.enableLogging();
        return map;
    }

    public static void cleanupGlobals(PyStringMap map) {
        map.disableLogging();
    }


    public static OdbStringIO StringIO() {
        return new OdbStringIO();
    }

    public static _odb_test test_odb(){
        return new _odb_test();
    }

}
