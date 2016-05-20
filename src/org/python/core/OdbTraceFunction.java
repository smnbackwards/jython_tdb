package org.python.core;

import org.magicwerk.brownies.collections.primitive.LongBigList;
import org.python.modules._odb.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;

/**
 * Created by dual- on 2/18/2016.
 */
public class OdbTraceFunction extends PythonTraceFunction {

    //    protected static int LEVEL = Py.MESSAGE;
    protected static int LEVEL = Py.COMMENT;

    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static LongBigList events = new LongBigList();
    protected static ArrayList<OdbException> exceptions = new ArrayList<>();
    protected static OdbFrame parent = null;
    protected static HistoryMap<Object, PyObject> globals = null;

    protected static int currentFrameId = 0;
    protected static int currentTimestamp = 0;
    protected static boolean enabled = false;
    protected static boolean replaying = false;

    protected static long callDepth = 0;
    protected static boolean waitForMainPyFile = true;

    OdbTraceFunction(PyObject tracefunc) {
        super(tracefunc);
    }

    public static boolean isEnabled(){
        return enabled;
    }

    public static boolean isReplaying(){
        return replaying;
    }

    public static void setEnabled(boolean enabled) {
        OdbTraceFunction.enabled = enabled;
    }

    public static void setReplaying(boolean replaying) {
        OdbTraceFunction.replaying = replaying;
    }

    public static LongBigList getEvents() {
        return events;
    }

    public static OdbEvent.EVENT_TYPE getCurrentEventType(){
        return OdbEvent.decodeEventType(events.get(currentTimestamp));
    }

    public static int getCurrentEventLineno(){
        return OdbEvent.decodeEventLineno(events.get(currentTimestamp));
    }

    public static OdbException getCurrentException(){
        for (OdbException e: exceptions) {
            if(e.timestamp == currentTimestamp){
                return e;
            }
        }
        return null;
    }

    public static int getCurrentTimestamp() {
        return currentTimestamp;
    }

    public static void setCurrentTimestamp(int timestamp){
        currentTimestamp = timestamp;
    }

    public static Stack<OdbFrame> getFrames() {
        return frames;
    }

    public static OdbFrame getCurrentFrame(){
        return frames.get(currentFrameId);
    }

    public static int getCurrentFrameId() {
        return currentFrameId;
    }

    public static void moveToTimestamp(int timestamp){
        if (timestamp >= 0 && timestamp < events.size()) {
            currentTimestamp = timestamp;
            currentFrameId = OdbEvent.decodeEventFrameId(events.get(currentTimestamp));
        }
    }

    public static void moveToFrame(OdbFrame frame){
        if(frame != null){
            moveToFrame(frame.index);
        }
    }

    public static void moveToFrame(int frameId){
        if (frameId >= 0 && frameId < frames.size()) {
            currentFrameId = frameId;
            currentTimestamp = frames.get(currentFrameId).timestamp;
        }
    }

    public static PyStringMap getLocals() {
        return getCurrentFrame().getLocals(currentTimestamp);
    }

    public static HistoryMap<Object, PyObject> getGlobals(){
        return globals;
    }


    public static void reset() {
        currentTimestamp = 0;
        currentFrameId = 0;
        frames = new Stack<>();
        events = new LongBigList();
        parent = null;

        enabled = false;
        replaying = false;
        callDepth = 0;
        waitForMainPyFile = true;
    }

    public static void setup() {
        currentTimestamp = 0;
        currentFrameId = 0;
        enabled = false;
        replaying = true;
    }

    public static void initializeParent(PyFrame frame) {
        if (parent == null && frames.empty()) {
            assert currentTimestamp == 0;
            assert frames.isEmpty();
            assert currentFrameId == 0;
            assert events.isEmpty();

            PyFrame parentFrame = frame;

            assert frame.f_globals instanceof PyStringMap;
            assert frame.f_globals == frame.f_locals;

            Map<Object, PyObject> tempMap = ((PyStringMap) frame.f_globals).getMap();
            globals = ((OdbMap<Object, PyObject>) tempMap).getHistoryMap();

            parent = new OdbFrame(frames.size(), 0, //Flag to say this is the enclosing frame
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
        HistoryMap<Object, PyObject> localMap = ((OdbMap<Object, PyObject>) ((PyStringMap) frame.getLocals()).getMap()).getHistoryMap();

        initializeParent(frame.f_back);

        parent = new OdbFrame(frames.size(), currentTimestamp,
                frame.f_code.co_filename,
                frame.f_back.f_lineno,
                frame.f_code.co_name,
                parent,
                localMap);

        frames.push(parent);
        currentFrameId = frames.size() - 1;

        events.add(OdbEvent.createEvent(frame.f_lineno, parent.index, OdbEvent.EVENT_TYPE.CALL));

        Py.maybeWrite("TTD call", frames.peek().toString() + " at " + currentTimestamp, LEVEL);
        currentTimestamp++;
    }

    protected static void returnEvent(int lineno, PyObject returnValue){
        if (!frames.empty()) {
            Py.maybeWrite("TTD return", parent.toString() + " at " + currentTimestamp, LEVEL);
            events.add(OdbEvent.createEvent(lineno, parent.index, OdbEvent.EVENT_TYPE.RETURN)); //TODO

            parent.return_timestamp = currentTimestamp;
            parent.return_value = returnValue;
            parent = parent.parent;
            //Find the matching frame index
            currentFrameId = parent == null ? 0 : parent.index;
            currentTimestamp++;
        }
    }

    protected static void returnEvent(PyFrame frame, PyObject returnValue) {
        returnEvent(frame.f_lineno, returnValue);
    }

    protected static void lineEvent(PyFrame frame) {
        initializeParent(frame);
        Py.maybeWrite("TTD line", frame.f_lineno + " at " + currentTimestamp, LEVEL);
        events.add(OdbEvent.createEvent(frame.f_lineno, parent.index, OdbEvent.EVENT_TYPE.LINE));
        currentTimestamp++;
    }

    protected static void exceptionEvent(PyFrame frame, PyObject type, PyObject value, PyObject traceback) {
        initializeParent(frame);
        Py.maybeWrite("TTD exception", value.toString() + " at " + currentTimestamp, LEVEL);
        events.add(OdbEvent.createEvent(frame.f_lineno, parent.index, OdbEvent.EVENT_TYPE.EXCEPTION)); //TODO exception list!
        exceptions.add(new OdbException(currentTimestamp, type, value, traceback));
        currentTimestamp++;
    }

    public static void uncaughtExceptionEvent(PyBaseException exception) {
        OdbFrame frame = getCurrentFrame();
        int lineno = OdbEvent.decodeEventLineno(events.peekLast());

        // exception
        events.add(OdbEvent.createEvent(lineno, frame.index, OdbEvent.EVENT_TYPE.EXCEPTION)); //TODO traceback
        exceptions.add(new OdbException(currentTimestamp, exception.getType(), exception, Py.None));
        currentTimestamp++;

        //Return
        returnEvent(frame.lineno, Py.None);
    }



    @Override
    protected TraceFunction safeCall(PyFrame frame, String label, PyObject arg) {
        synchronized (imp.class) {
            synchronized (this) {
                ThreadState ts = Py.getThreadState();
                if (ts.tracing)
                    return null;
                if (tracefunc == null)
                    return null;
                try {
                    ts.tracing = true;
                    if (frame.f_code.co_filename.startsWith(Py.getSystemState().path.get(1).toString())) {
                        enabled = false;
                        return this;
                    }

                    if (waitForMainPyFile) {
                        if (ts.frame.f_code.co_filename.equals("<string>")) {
                            return this;
                        }

                        if (label.equals("call")) {
                            waitForMainPyFile = false;
                            enabled = true;
                            return this;
                        }
                    }


                    if (callDepth == 0 && (label.equals("return") || label.equals("exception"))) {
                        if(enabled && label.equals("return")){
                            returnEvent(frame, arg);
                        }

                        if(enabled && label.equals("exception")){
                            PyTuple t = (PyTuple)arg;
                            exceptionEvent(frame, t.pyget(0), t.pyget(1), t.pyget(2));
                        }

                        enabled = false;
                        tracefunc = null;
                        return null;
                    }

                    if (label.equals("call")) {
                        callDepth++;
                        callEvent(frame);
                    }
                    if (label.equals("return")) {
                        callDepth--;
                        returnEvent(frame, arg);
                    }

                    if(label.equals("line")){
                        lineEvent(frame);
                    }

                    if(label.equals("exception")){
                        PyTuple t = (PyTuple)arg;
                        exceptionEvent(frame, t.pyget(0), t.pyget(1), t.pyget(2));
                    }

                } catch (PyException exc) {
                    frame.tracefunc = null;
                    ts.tracefunc = null;
                    ts.profilefunc = null;
                    throw exc;
                } finally {
                    ts.tracing = false;
                }
                return this;
            }
        }
    }

    @Override
    public TraceFunction traceCall(PyFrame frame) {
        return safeCall(frame, "call", Py.None);
    }

    @Override
    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        return safeCall(frame, "return", ret);
    }

    @Override
    public TraceFunction traceLine(PyFrame frame, int line) {
        return safeCall(frame, "line", Py.None);
    }

    @Override
    public TraceFunction traceException(PyFrame frame, PyException exc) {
        // We must avoid passing a null to a PyTuple
        PyObject safeTraceback = exc.traceback == null ? Py.None : exc.traceback;
        return safeCall(frame, "exception",
                new PyTuple(exc.type, exc.value, safeTraceback));
    }
}
