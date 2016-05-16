package org.python.modules._odb;

import org.python.core.*;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {
//    private static int LEVEL = Py.MESSAGE;
    private static int LEVEL = Py.COMMENT;

    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static LinkedList<OdbEvent> eventHistory = new LinkedList<>();
    protected static OdbFrame parent = null;
    protected static HistoryMap<Object,PyObject> globals = null;

    protected static int currentFrameId = -1;
    public static int currentTimestamp = 0;
    public static boolean enabled = false;
    public static boolean replaying = false;

    public static PyStringMap initializeGlobals(PyStringMap map){
        //PyStringMap is not exposed to python in Jython
        //So we include this helper function to call the enableLogging method on it
        map.enableLogging();
        return map;
    }

    public static void cleanupGlobals(PyStringMap map){
        map.disableLogging();
    }

    public static void initializeParent(PyFrame frame){
        if(parent == null){
            assert currentTimestamp == 0;
            assert frames.isEmpty();
            assert eventHistory.isEmpty();

            PyFrame parentFrame = frame;

            assert frame.f_globals instanceof PyStringMap;
            assert frame.f_globals == frame.f_locals;

            Map<Object, PyObject> tempMap = ((PyStringMap)frame.f_globals).getMap();
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

        frames.push(parent);

        eventHistory.add(new OdbEvent(currentTimestamp, frame.f_lineno, parent, OdbEvent.Type.CALL));

        Py.maybeWrite("TTD call", frames.peek().toString() + " at " + currentTimestamp,LEVEL);
        currentTimestamp++;
        //The next frame will be at the top of the stack
        currentFrameId = frames.size()-1;
    }

    public static void returnEvent(PyFrame frame, PyObject returnValue) {
        if (!frames.empty()) {
            Py.maybeWrite("TTD return", parent.toString() + " at "+ currentTimestamp, LEVEL);
            eventHistory.add(new OdbEvent(currentTimestamp, frame.f_lineno, parent, OdbEvent.Type.RETURN));
            parent.returnTimestamp = currentTimestamp;
            parent.returnValue = returnValue;
            parent = parent.parent;
            //Find the matching frame index
            currentFrameId = frames.indexOf(parent);
            currentTimestamp++;
        }
    }

    public static void lineEvent(PyFrame frame) {
        initializeParent(frame);
        Py.maybeWrite("TTD line", frame.f_lineno + " at "+ currentTimestamp, LEVEL);
        eventHistory.add(new OdbEvent(currentTimestamp, frame.f_lineno, parent, OdbEvent.Type.LINE));
        currentTimestamp++;
    }

    public static void exceptionEvent(PyFrame frame, PyObject type, PyObject value, PyObject traceback) {
        initializeParent(frame);
        Py.maybeWrite("TTD exception", value.toString() + " at "+ currentTimestamp, LEVEL);
        eventHistory.add(new OdbExceptionEvent(currentTimestamp, frame.f_lineno, parent, type, value, traceback));
        currentTimestamp++;
    }

    public static void uncaghtExceptionEvent(PyBaseException exception){
        OdbFrame frame = getCurrentFrame();
        int lineno = eventHistory.peekLast().lineno;

        // exception
        eventHistory.add(new OdbExceptionEvent(currentTimestamp, lineno, frame, exception.getType(), exception, Py.None)); //TODO traceback
        currentTimestamp++;

        //Return
        eventHistory.add(new OdbEvent(currentTimestamp, lineno, frame, OdbEvent.Type.RETURN));
        parent.returnTimestamp = currentTimestamp;
        parent.returnValue = Py.None;
        parent = parent.parent;
        //Find the matching frame index
        currentFrameId = frames.indexOf(parent);
        currentTimestamp++;
    }

    public static void reset(){
        currentTimestamp = 0;
        currentFrameId = -1;
        frames = new Stack<>();
        eventHistory = new LinkedList<>();
        parent = null;

        enabled = false;
        replaying = false;

        //TODO remove dependency on this TraceFunction class / create more generic
        TdbTraceFunction.resetInstructionCount();
    }

    public static void setup(){
        currentTimestamp = 0;
        currentFrameId = 0;
        enabled = false;
        replaying = true;
    }

    public static List<OdbEvent> getEvents() {
        return eventHistory;
    }

    public static List<OdbFrame> getFrames(){
        return frames;
    }

    public static int getCurrentTimestamp() {
        return currentTimestamp;
    }

    public static OdbEvent getCurrentEvent() {
        return eventHistory.get(currentTimestamp);
    }

    public static OdbFrame getCurrentFrame() {
//        return getCurrentEvent().frame;
        return frames.get(currentFrameId);
    }

    public static int getCurrentFrameId(){
        return currentFrameId;
    }

    public static PyStringMap getCurrentLocals(){
        if(getCurrentFrame().locals == globals){
            return getGlobals();
        }
        return getCurrentFrame().getLocals(currentTimestamp);
    }

    public static PyStringMap getGlobals(){
        PyStringMap map = new PyStringMap();
        for( Map.Entry<Object, HistoryValueList<PyObject>> e : globals.map.entrySet()){
            HistoryValue<PyObject> value = e.getValue().getHistoryValue(currentTimestamp);
            if(value != null && value.getValue() != null && !e.getKey().equals("__builtins__")){
                map.__setitem__((String)e.getKey(), value.getValue());
            }
        }
        return map;
    }

    public static List<HistoryValue<PyObject>> getLocalHistory(String key) {
        return getCurrentFrame().locals.getBefore(currentTimestamp, key);
    }

    public static PyStringMap getFrameArguments(){
        OdbFrame frame = getCurrentFrame();
        return frame.getLocals(frame.timestamp);
    }

    public static void do_step(){
        do_jump(currentTimestamp+1);
    }

    public static void do_rstep(){
        do_jump(currentTimestamp-1);
    }

    public static void do_return(){
        OdbFrame frame = getCurrentFrame();

        //if I am the return event I am looking for then just step 1
        if (getCurrentEvent().eventType == OdbEvent.Type.RETURN
                && getCurrentEvent().frame.equals(frame)) {
            do_step();
            return;
        }

        do_jump(frame.returnTimestamp);
    }

    public static void do_rreturn(){
        OdbEvent event = null;
        OdbFrame frame = getCurrentFrame();
        if(frame.timestamp == getCurrentTimestamp()){
            do_rstep();
        } else {
            do_jump(frame.timestamp);
        }
    }

    public static void do_next(){
        OdbEvent event = null;
        OdbFrame frame = getCurrentFrame();

        if(getCurrentEvent().eventType == OdbEvent.Type.RETURN){
            frame = frame.parent;
        }

        ListIterator<OdbEvent> it = eventHistory.listIterator(getCurrentTimestamp()+1);
        while(it.hasNext()){
            event = it.next();
            if(event.frame.equals(frame)){
                break;
            }
            event = null;
        }

        if(event != null){
            do_jump(event.timestamp);
        }
    }

    public static void do_rnext(){
        OdbEvent event = null;
        OdbFrame frame = getCurrentFrame();

        if(getCurrentEvent().eventType == OdbEvent.Type.CALL){
            frame = frame.parent;
        }

        ListIterator<OdbEvent> it = eventHistory.listIterator(getCurrentTimestamp());
        while(it.hasPrevious()){
            event = it.previous();
            if(event.frame.equals(frame)){
                break;
            }
            event = null;
        }

        if(event != null){
            do_jump(event.timestamp);
        }
    }


    public static void do_jump(int n){
        if(n >= 0 && n < eventHistory.size()){
            currentTimestamp = n;
            currentFrameId = frames.indexOf(getCurrentEvent().frame);
        }
    }

    public static void moveUpFrames() {
        OdbFrame frame = getCurrentFrame();
        if(frame == null){
            return;
        }
        if(frame.parent != null){
            //Use parent to climb up call stack
            currentFrameId = frames.indexOf(frame.parent);
            currentTimestamp = frames.get(currentFrameId).timestamp;
        }
    }

    public static void moveDownFrames() {
        //We don't keep child links and the next frame is not guaranteed to be a child
        //Scan the events log to find a CALL event which has a frame with the current frame as its parent
        OdbFrame frame = getCurrentFrame();
        if(frame == null){
            return;
        }
        OdbEvent event = null;
        for (int i = currentTimestamp; i < eventHistory.size(); i++){
            event = eventHistory.get(i);
            if(event.frame != null && event.frame.parent == frame){
                break;
            }
            event = null;
        }

        if(event != null) {
            currentTimestamp = event.timestamp;
            currentFrameId = frames.indexOf(event.frame);
        }
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

}
