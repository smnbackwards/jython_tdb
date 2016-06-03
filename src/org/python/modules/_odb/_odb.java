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
    protected static BreakpointManager breakpointManager = new BreakpointManager();

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

    public static String getEventType(int timestamp){
        return OdbEvent.decodeEventType(OdbTraceFunction.getEvents().get(timestamp)).toString();
    }

    public static OdbFrame getEventFrame(int timestamp){
        return OdbTraceFunction.getFrames().get(OdbEvent.decodeEventFrameId(OdbTraceFunction.getEvents().get(timestamp)));
    }

    public static void reset() {
        breakpointManager = new BreakpointManager();
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
            OdbFrame frame = OdbTraceFunction.getFrames().get(frameid);
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

    public static List<OdbFrame> getFrames(int startFrameId, int endFrameId){
        Stack<OdbFrame> frames = OdbTraceFunction.getFrames();
        int end = Math.min(frames.size(), endFrameId);
        return frames.subList(startFrameId, end);
    }

    public static PyStringMap getCurrentLocals() {
        if (getCurrentFrame().locals == OdbTraceFunction.getGlobals()) {
            return getGlobals();
        }
        return OdbTraceFunction.getLocals();
    }

    public static PyStringMap getGlobals() {
        return getGlobalsAt(getCurrentTimestamp());
    }

    public static PyStringMap getGlobalsAt(int timestamp) {
        PyStringMap map = new PyStringMap();
        for (Map.Entry<Object, HistoryValueList<PyObject>> e : OdbTraceFunction.getGlobals().map.entrySet()) {
            HistoryValue<PyObject> value = e.getValue().getHistoryValue(timestamp);
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

    public static List<String> eval(String cmd){
        List<String> results = new ArrayList<>();
        // pre-compile the code to improve performance
        PyCode code = __builtin__.evalCompile(cmd);
        OdbFrame frame = getCurrentFrame();
        LongBigList events = OdbTraceFunction.getEvents();
        Stack<OdbFrame> frames = OdbTraceFunction.getFrames();
        String filename = frame.filename;
        long eventLong = events.get(getCurrentTimestamp());
        int lineno = OdbEvent.decodeEventLineno(eventLong);
        int end = frame.return_timestamp;
        PyObject last_result = null;
        String pad = "          ";
        for (int i = frame.timestamp + 1; i < end; i++) {
            eventLong = events.get(i);
            frame = frames.get(OdbEvent.decodeEventFrameId(eventLong));
            //Call events mean we have a new frame, so skip to the return
            if(OdbEvent.decodeEventType(eventLong) == OdbEvent.EVENT_TYPE.CALL
                    // And this isn't the same function
                    && !(frame.filename.equals(filename)
                            && OdbEvent.decodeEventLineno(eventLong) == lineno )
                    ){
                i = frame.return_timestamp;
                continue;
            }

            try {
                //Evaluate the code using the locals and globals at the correct time
                PyObject result = __builtin__.eval(code, getGlobalsAt(i), frame.getLocals(i));
                if (!result.equals(last_result)) {
                    last_result = result;
                    results.add(String.format("<%s> %s : %s", i, pad.substring((int)Math.log10(i)), result.toString()));
                }

            } catch (Exception e) {
                //Don't print the exceptions. There are a lot of name errors when variables don't exist
            }

        }
        return results;
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

        return do_jump(firstBreakpointBetween(getCurrentTimestamp(), getCurrentFrame().return_timestamp));
    }

    public static int do_rreturn() {
        OdbFrame frame = getCurrentFrame();
        int currentTimestamp = getCurrentTimestamp();
        if (frame.timestamp == currentTimestamp) {
            return do_rstep();
        } else {
            return do_jump(lastBreakpointBetween(frame.timestamp, currentTimestamp));
        }
    }

    public static int do_next() {
        OdbFrame frame = getCurrentFrame();

        if (OdbTraceFunction.getCurrentEventType() == OdbEvent.EVENT_TYPE.RETURN) {
            frame = frame.parent;
        }

        if(frame == null){
            return getCurrentTimestamp();
        }

        LongBigList events = OdbTraceFunction.getEvents();
        List<Integer> breakpointLines = breakpointManager.getBreakpointLinesForFile(frame.filename);

        for (int i = getCurrentTimestamp() + 1; i < events.size(); i++) {
            long eventlong = events.get(i);
            int frameId = OdbEvent.decodeEventFrameId(eventlong);
            int lineno = OdbEvent.decodeEventLineno(eventlong);
            if (frameId == frame.index || breakpointLines.contains(lineno)) {
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

        if(frame == null){
            return getCurrentTimestamp();
        }

        LongBigList events = OdbTraceFunction.getEvents();
        List<Integer> breakpointLines = breakpointManager.getBreakpointLinesForFile(frame.filename);

        for (int i = getCurrentTimestamp() - 1; i >= 0; i--) {
            long eventlong = events.get(i);
            int frameId = OdbEvent.decodeEventFrameId(eventlong);
            int lineno = OdbEvent.decodeEventLineno(eventlong);
            if (frameId == frame.index || breakpointLines.contains(lineno)) {
                return do_jump(i);
            }
        }
        return getCurrentTimestamp();
    }


    public static int do_jump(int n) {
        OdbTraceFunction.moveToTimestamp(n);
        return getCurrentTimestamp();
    }

    public static int do_continue(){
        int stopTime = firstBreakpointBetween(getCurrentTimestamp(), OdbTraceFunction.getEvents().size()-1);
        return do_jump(stopTime);
    }

    public static int do_rcontinue(){
        int stopTime = lastBreakpointBetween(0, getCurrentTimestamp());
        return do_jump(stopTime);
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

    private static int firstBreakpointBetween(int startTimestamp, int endTimestamp){
        if(startTimestamp == endTimestamp){
            return endTimestamp;
        }

        LongBigList events = OdbTraceFunction.getEvents();
        long eventLong = events.get(startTimestamp);
        int frameId = OdbEvent.decodeEventFrameId(eventLong);
        String filename = OdbTraceFunction.getFrames().get(frameId).filename; //TODO continue could change files
        List<Integer> lineNumbers = breakpointManager.getBreakpointLinesForFile(filename);

        if(lineNumbers.isEmpty()){
            return endTimestamp;
        }

        for (int i = startTimestamp + 1 ; i < endTimestamp; i++ ) {
            eventLong = events.get(i);
            if (lineNumbers.contains(OdbEvent.decodeEventLineno(eventLong))){
                return i;
            }
        }

        return endTimestamp;
    }

    private static int lastBreakpointBetween(int startTimestamp, int endTimestamp){
        if(startTimestamp == endTimestamp){
            return endTimestamp;
        }

        LongBigList events = OdbTraceFunction.getEvents();
        long eventLong = events.get(startTimestamp);
        int frameId = OdbEvent.decodeEventFrameId(eventLong);
        String filename = OdbTraceFunction.getFrames().get(frameId).filename;
        List<Integer> lineNumbers = breakpointManager.getBreakpointLinesForFile(filename);

        if(lineNumbers.isEmpty()){
            return startTimestamp;
        }

        for (int i = endTimestamp - 1; i >= 0; i--) {
            eventLong = events.get(i);
            if (lineNumbers.contains(OdbEvent.decodeEventLineno(eventLong))){
                return i;
            }
        }

        return 0;
    }

    public static String setBreakpoint(String filename, int lineno){
        return breakpointManager.insert(filename, lineno).toString();
    }

    public static Collection<Breakpoint> getBreakpoints(){
        return breakpointManager.getBreakpoints();
    }

    public static void clearAllBreakpoints(){
        breakpointManager.clearAll();
    }

    public static String clearBreakpoint(String filename, int lineno){
        if(breakpointManager.clear(filename, lineno)){
            return null;
        }
        return String.format("There is no breakpoint at %s:%d", filename, lineno);
    }

    public static String clearBreakpointNumber(int index){
        if(!breakpointManager.checkIndex(index)){
            return String.format("No breakpoint numbered %d", index);
        }

        if(breakpointManager.clear(index)){
            return null;
        }

        return String.format("Breakpoint with index %d already deleted", index);
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
