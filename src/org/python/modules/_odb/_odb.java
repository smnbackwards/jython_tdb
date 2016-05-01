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

    protected static int currentFrameId = -1;
    public static int currentTimestamp = 0;
    public static boolean enabled = false;
    public static boolean replaying = false;


    public static void initializeParent(PyFrame frame){
        if(parent == null){
            //TODO -ea isn't enabled!!!
            assert currentTimestamp == 0;
            assert frames.isEmpty();
            assert eventHistory.isEmpty();

            PyFrame parentFrame = frame;
            HistoryMap<Object> parentLocalMap = new HistoryMap<>();
            Map<Object, PyObject> tempMap = ((PyStringMap)frame.getLocals()).getMap();
            for (Object key :tempMap.keySet() ) {
                if(!key.equals("__builtins__")){
                    parentLocalMap.put(-1, key, tempMap.get(key));
                }
            }
            parent = new OdbFrame(0, //Flag to say this is the enclosing frame
                    parentFrame.f_code.co_filename,
                    parentFrame.f_back.f_lineno,
                    parentFrame.f_code.co_name,
                    null,
                    parentLocalMap);
            frames.push(parent);
            currentFrameId++;
        }
    }

    public static void callEvent(PyFrame frame) {
        HistoryMap<Object> localMap = new HistoryMap<>();
        Map<Object, PyObject> tempMap = ((PyStringMap)frame.getLocals()).getMap();
        tempMap.keySet().stream().forEach(o -> localMap.put(currentTimestamp, o, tempMap.get(o)));

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

    public static void localEvent(String index, PyObject value){

        if(parent == null){
            //allows us to test certain features without invoking the debugger
            return;
        }

        OdbFrame frame = getCurrentFrame();
        if(frame != null) {
            Py.maybeWrite("TTD local", String.format("Set %s to %s in %s at %s", index, value, frame, currentTimestamp), LEVEL);
            frame.locals.put(currentTimestamp, index, value);
        } else {
            Py.maybeWrite("TTD", "localEvent, NO FRAME", LEVEL);
        }
    }

    public static void globalEvent(String name, PyObject value) {
        Py.maybeWrite("TTD", String.format("Set global %s to %s", name, value), LEVEL);

//        OdbFrame frame = frames.get(0);
//        if(frame != null){
//            frame.locals.put(currentTimestamp, name, value);
//        }
    }

    public static void reset(){
        currentTimestamp = 0;
        currentFrameId = -1;
        frames = new Stack<>();
        eventHistory = new LinkedList<>();
        parent = null;


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
        return getCurrentFrame().getLocals(currentTimestamp);
    }

    public static PyObject lookupLocal(String key){
        return getCurrentFrame().locals.get(currentTimestamp, key);
    }

    public static PyObject lookupLocalField(String local, String field){
        return ((PyInstance)lookupLocal(local)).historyMap.get(currentTimestamp, field);
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
