package org.python.modules._odb;

import org.magicwerk.brownies.collections.BigList;
import org.magicwerk.brownies.collections.helper.BigLists;
import org.magicwerk.brownies.collections.primitive.LongBigList;
import org.python.core.*;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {
    //    private static int LEVEL = Py.MESSAGE;
    private static int LEVEL = Py.COMMENT;

    protected static Stack<OdbFrame> frames = new Stack<>();
    //    protected static BigList<OdbEvent> eventHistory = new BigList<>();
    protected static LongBigList events = new LongBigList();
    protected static OdbFrame parent = null;
    protected static HistoryMap<Object, PyObject> globals = null;

    protected static int parentIndex = -1;

    protected static int currentFrameId = 0;
    public static int currentTimestamp = 0;
    public static boolean enabled = false;
    public static boolean replaying = false;

    public static PyStringMap initializeGlobals(PyStringMap map) {
        //PyStringMap is not exposed to python in Jython
        //So we include this helper function to call the enableLogging method on it
        map.enableLogging();
        return map;
    }

    public static void cleanupGlobals(PyStringMap map) {
        map.disableLogging();
    }

    public static void checkConsistency(){
        if(currentFrameId >= 0) {

        }
    }

    public static void initializeParent(PyFrame frame) {
        if (parent == null && frames.empty()) {
            assert currentTimestamp == 0;
            assert frames.isEmpty();
            assert currentFrameId == 0;
//            assert eventHistory.isEmpty();

            PyFrame parentFrame = frame;

            assert frame.f_globals instanceof PyStringMap;
            assert frame.f_globals == frame.f_locals;

            Map<Object, PyObject> tempMap = ((PyStringMap) frame.f_globals).getMap();
            globals = ((OdbMap<Object, PyObject>) tempMap).historyMap;

            parent = new OdbFrame(0, //Flag to say this is the enclosing frame
                    parentFrame.f_code.co_filename,
                    parentFrame.f_back.f_lineno,
                    parentFrame.f_code.co_name,
                    null,
                    globals);
            frames.push(parent);
            currentFrameId++;
        }
    }

    public static void callEvent(PyFrame frame) {
        //This event is called from code being debugged
        //which means logging should be enabled
        //so the locals dictionary is backed by an OdbMap
        HistoryMap<Object, PyObject> localMap = ((OdbMap<Object, PyObject>) ((PyStringMap) frame.getLocals()).getMap()).historyMap;

        initializeParent(frame.f_back);

        parent = new OdbFrame(currentTimestamp,
                frame.f_code.co_filename,
                frame.f_back.f_lineno,
                frame.f_code.co_name,
                parent,
                localMap);

        //flag to speedup lookups without taking up extra space
        parent.return_timestamp = -1 * frames.size();

        frames.push(parent);
        currentFrameId = frames.size() - 1;

//        eventHistory.add(new OdbCallEvent(frame.f_lineno, parent));
        events.add(OdbEvent.createEvent(frame.f_lineno, frames.size() - frames.search(parent), OdbEvent.EVENT_TYPE.CALL));

        Py.maybeWrite("TTD call", frames.peek().toString() + " at " + currentTimestamp, LEVEL);
        currentTimestamp++;
    }

    public static void returnEvent(PyFrame frame, PyObject returnValue) {
        if (!frames.empty()) {
            Py.maybeWrite("TTD return", parent.toString() + " at " + currentTimestamp, LEVEL);
//            eventHistory.add(new OdbReturnEvent(frame.f_lineno, parent));
            events.add(OdbEvent.createEvent(frame.f_lineno, frames.size() - frames.search(parent), OdbEvent.EVENT_TYPE.RETURN)); //TODO

            parent.return_timestamp = currentTimestamp;
            parent.return_value = returnValue;
            parent = parent.parent;
            //Find the matching frame index
            currentFrameId = parent == null ? 0 : -parent.return_timestamp;
            currentTimestamp++;
        }
    }

    public static void lineEvent(PyFrame frame) {
        initializeParent(frame);
        Py.maybeWrite("TTD line", frame.f_lineno + " at " + currentTimestamp, LEVEL);
//        eventHistory.add(new OdbLineEvent(frame.f_lineno, parent));
        events.add(OdbEvent.createEvent(frame.f_lineno, frames.size() - frames.search(parent), OdbEvent.EVENT_TYPE.LINE));
        currentTimestamp++;
    }

    public static void exceptionEvent(PyFrame frame, PyObject type, PyObject value, PyObject traceback) {
        initializeParent(frame);
        Py.maybeWrite("TTD exception", value.toString() + " at " + currentTimestamp, LEVEL);
//        eventHistory.add(new OdbExceptionEvent(frame.f_lineno, parent, type, value, traceback));
        events.add(OdbEvent.createEvent(frame.f_lineno, frames.size() - frames.search(parent), OdbEvent.EVENT_TYPE.EXCEPTION)); //TODO exception list!
        currentTimestamp++;
    }

    public static void uncaghtExceptionEvent(PyBaseException exception) {
//        OdbFrame frame = getCurrentFrame();
//        int lineno = eventHistory.peekLast().lineno;
//
//        // exception
//        eventHistory.add(new OdbExceptionEvent(lineno, frame, exception.getType(), exception, Py.None)); //TODO traceback
//        //TODO events
//        currentTimestamp++;
//
//        //Return
//        eventHistory.add(new OdbReturnEvent(lineno, frame));
//        //TODO events
//        parent.return_timestamp = currentTimestamp;
//        parent.return_value = Py.None;
//        parent = parent.parent;
//        //Find the matching frame index
//        currentFrameId = parent == null ? -1 : -parent.return_timestamp;
//        currentTimestamp++;
    }

    public static void reset() {
        currentTimestamp = 0;
        currentFrameId = 0;
        frames = new Stack<>();
//        eventHistory = new BigList<>();
        events = new LongBigList();
        parent = null;

        enabled = false;
        replaying = false;

        //TODO remove dependency on this TraceFunction class / create more generic
        TdbTraceFunction.resetInstructionCount();
    }

    public static void setup() {
        currentTimestamp = 0;
        currentFrameId = 0;
        enabled = false;
        replaying = true;
    }

    public static List<OdbEvent> getEvents() {
//        return eventHistory;
        return null; //TODO
    }

    public static List<OdbFrame> getFrames() {
        return frames;
    }

    public static int getCurrentTimestamp() {
        return currentTimestamp;
    }

    public static OdbEvent getCurrentEvent() {
//        OdbEvent event = eventHistory.get(currentTimestamp);
        long eventLong = events.get(currentTimestamp);
        int lineno = OdbEvent.decodeEventLineno(eventLong);
        int frameId = OdbEvent.decodeEventFrameId(eventLong);
        OdbFrame frame = frames.get(frameId);

        switch (OdbEvent.decodeEventType(eventLong)) {

            case LINE:
                return new OdbLineEvent(lineno, frame);
            case CALL:
                return new OdbCallEvent(lineno, frame);
            case RETURN:
                return new OdbReturnEvent(lineno, frame);
            case EXCEPTION:
            default:
                return null; //TODO exception
        }
    }

    public static OdbFrame getCurrentFrame() {
//        return getCurrentEvent().frame;
        return frames.get(currentFrameId);
    }

    public static int getCurrentFrameId() {
        return currentFrameId;
    }

    public static PyStringMap getCurrentLocals() {
        if (getCurrentFrame().locals == globals) {
            return getGlobals();
        }
        return getCurrentFrame().getLocals(currentTimestamp);
    }

    public static PyStringMap getGlobals() {
        PyStringMap map = new PyStringMap();
        for (Map.Entry<Object, HistoryValueList<PyObject>> e : globals.map.entrySet()) {
            HistoryValue<PyObject> value = e.getValue().getHistoryValue(currentTimestamp);
            if (value != null && value.getValue() != null && !e.getKey().equals("__builtins__")) {
                map.__setitem__((String) e.getKey(), value.getValue());
            }
        }
        return map;
    }

    public static List<HistoryValue<PyObject>> getLocalHistory(String key) {
        return getCurrentFrame().locals.getBefore(currentTimestamp, key);
    }

    public static PyStringMap getFrameArguments() {
        OdbFrame frame = getCurrentFrame();
        return frame.getLocals(frame.timestamp);
    }

    public static void do_step() {
        do_jump(currentTimestamp + 1);
    }

    public static void do_rstep() {
        do_jump(currentTimestamp - 1);
    }

    public static void do_return() {
        OdbFrame frame = getCurrentFrame();

        //if I am the return event I am looking for then just step 1
        if (getCurrentEvent() instanceof OdbReturnEvent
                && getCurrentEvent().frame.equals(frame)) {
            do_step();
            return;
        }

        do_jump(frame.return_timestamp);
    }

    public static void do_rreturn() {
        OdbEvent event = null;
        OdbFrame frame = getCurrentFrame();
        if (frame.timestamp == getCurrentTimestamp()) {
            do_rstep();
        } else {
            do_jump(frame.timestamp);
        }
    }

    public static void do_next() {
        OdbFrame frame = getCurrentFrame();

        if (getCurrentEvent() instanceof OdbReturnEvent) {
            frame = frame.parent;
        }

        int frameid = frames.size() - frames.search(frame);

        for (int i = currentTimestamp + 1; i < events.size(); i++) {
            long eventlong = events.get(i);
            if(OdbEvent.decodeEventFrameId(eventlong) == frameid){
                do_jump(i);
                return;
            }
        }
    }

    public static void do_rnext() {
        OdbFrame frame = getCurrentFrame();

        if (getCurrentEvent() instanceof OdbCallEvent) {
            frame = frame.parent;
        }

        int frameid = frames.size() - frames.search(frame);

        for (int i = currentTimestamp-1; i >= 0; i--) {
            long eventlong = events.get(i);
            if(OdbEvent.decodeEventFrameId(eventlong) == frameid){
                do_jump(i);
                return;
            }
        }
    }


    public static void do_jump(int n) {
        if (n >= 0 && n < events.size()) {
            currentTimestamp = n;
            currentFrameId = frames.indexOf(getCurrentEvent().frame);
        }
    }

    public static void moveUpFrames() {
        OdbFrame frame = getCurrentFrame();
        if (frame == null) {
            return;
        }
        if (frame.parent != null) {
            //Use parent to climb up call stack
            currentFrameId = frames.indexOf(frame.parent);
            currentTimestamp = frames.get(currentFrameId).timestamp;
        }
    }

    public static void moveDownFrames() {
//        //We don't keep child links and the next frame is not guaranteed to be a child
//        //Scan the events log to find a CALL event which has a frame with the current frame as its parent
//        OdbFrame frame = getCurrentFrame();
//        if (frame == null) {
//            return;
//        }
//        OdbEvent event = null;
//        int timestamp = -1;
//        for (int i = currentTimestamp; i < eventHistory.size(); i++) {
//            event = eventHistory.get(i);
//            if (event.frame != null && event.frame.parent == frame) {
//                timestamp = i;
//                break;
//            }
//            event = null;
//        }
//
//        if (timestamp >= 0) {
//            currentTimestamp = timestamp;
//            currentFrameId = frames.indexOf(event.frame);
//        }
    }

    public static void moveNextFrames() {
        if (currentFrameId < frames.size() - 1) {
            currentFrameId++;
            currentTimestamp = frames.get(currentFrameId).timestamp;
        }
    }

    public static void movePrevFrames() {
        if (currentFrameId > 0) {
            currentFrameId--;
            currentTimestamp = frames.get(currentFrameId).timestamp;
        }
    }


    public static OdbStringIO StringIO() {
        return new OdbStringIO();
    }

}
