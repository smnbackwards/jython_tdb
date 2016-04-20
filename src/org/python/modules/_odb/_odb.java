package org.python.modules._odb;

import org.python.core.*;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {
    private static int LEVEL = Py.MESSAGE;
//    private static int LEVEL = Py.COMMENT;

    protected static int timestamp = 0;
    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static LinkedList<OdbEvent> eventHistory = new LinkedList<>();
    protected static OdbFrame parent = null;

    protected static int currentFrame = -1;
    protected static int currentTimestamp = -1;


    public static void initializeParent(PyFrame frame){
        if(parent == null){
            assert timestamp == 0;
            assert frames.isEmpty();
            assert eventHistory.isEmpty();

            PyFrame parentFrame = frame;
            Map<Object, PyObject> parentLocalMap = ((PyStringMap)frame.getLocals()).getMap();
            parent = new OdbFrame(0, //Flag to say this is the enclosing frame
                    parentFrame.f_code.co_filename,
                    parentFrame.f_back.f_lineno,
                    parentFrame.f_code.co_name,
                    null,
                    parentLocalMap);
            frames.push(parent);
        }
    }

    public static void callEvent(PyFrame frame) {
        PyStringMap locals = (PyStringMap) frame.getLocals();
        Map<Object, PyObject> localMap = locals.getMap();

        initializeParent(frame.f_back);

        parent = new OdbFrame(timestamp,
                frame.f_code.co_filename,
                frame.f_back.f_lineno,
                frame.f_code.co_name,
                parent,
                localMap);

        frames.push(parent);

        eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.CALL));

        Py.maybeWrite("TTD", frames.peek().toString(),LEVEL);
        timestamp++;
    }

    public static void returnEvent(PyFrame frame) {
        if (!frames.empty()) {
            Py.maybeWrite("TTD", "Return from event: " + parent.toString(), LEVEL);
            eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.RETURN));
            parent = parent.parent;
            timestamp++;
        }
    }

    public static void lineEvent(PyFrame frame) {
        initializeParent(frame);
        eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.LINE));
        timestamp++;
    }

    public static void reset(){
        timestamp = 0;
        timestamp = 0;
        frames = new Stack<>();
        eventHistory = new LinkedList<>();
        parent = null;

        currentFrame = -1;
        currentTimestamp = -1;

        //TODO remove dependency on this TraceFunction class / create more generic
        TdbTraceFunction.resetInstructionCount();
    }

    public static void setup(){
        currentTimestamp = 0;
        currentFrame = 0;
    }

    public static List<OdbEvent> getEvents() {
        return eventHistory;
    }

    public static List<OdbFrame> getFrames(){
        return frames;
    }

    public static long getCurrentTimestamp() {
        return currentTimestamp;
    }

    public static OdbEvent getCurrentEvent() {
        return eventHistory.get(currentTimestamp);
    }

    public static OdbFrame getCurrentFrame() {
        return getCurrentEvent().frame;
//        return frames.elementAt(currentFrame);
    }

    public static int getCurrentFrameId(){
        return currentFrame;
    }

    public static void do_step(){
        do_jump(currentTimestamp+1);
    }

    public static void do_rstep(){
        do_jump(currentTimestamp-1);
    }

    public static void do_return(){
        //step through frames until you reach a frame equal to the current parent frame
        OdbFrame frame = null;
        OdbFrame parent = getCurrentFrame().parent;

        for (int i = getCurrentFrameId()+1; i <frames.size(); i++) {
            frame = frames.get(i);
            if(frame.equals(parent)){
                break;
            }
            frame = null;
        }

        if(frame != null){
            do_jump(frame.timestamp);
        }
    }

    public static void do_next(){
        OdbFrame frame = null;
        OdbFrame parent = getCurrentFrame().parent;

        for (int i = getCurrentFrameId()+1; i <frames.size(); i++) {
            frame = frames.get(i);
            if(frame.equals(parent)){
                break;
            }
            frame = null;
        }

        if(frame != null){
            do_jump(frame.timestamp);
        }
    }

    public static void do_rnext(){

    }

    public static void do_rreturn(){
        do_jump(0);
    }

    public static void do_jump(int n){
        if(n >= 0 && n < eventHistory.size()){
            currentTimestamp = n;
            currentFrame = frames.indexOf(getCurrentEvent().frame);
        }
    }

    public static void moveUpFrames() {
        OdbFrame frame = getCurrentFrame();
        if(frame == null){
            return;
        }
        if(frame.parent != null){
            //Use parent to climb up call stack
            currentFrame = frames.indexOf(frame.parent);
            currentTimestamp = frames.get(currentFrame).timestamp;
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
            currentFrame = frames.indexOf(event.frame);
        }
    }

    public static void moveNextFrames() {
        if (currentFrame < frames.size() - 1) {
            currentFrame++;
            currentTimestamp = frames.get(currentFrame).timestamp;
        }
    }

    public static void movePrevFrames() {
        if (currentFrame > 0) {
            currentFrame--;
            currentTimestamp = frames.get(currentFrame).timestamp;
        }
    }


}
