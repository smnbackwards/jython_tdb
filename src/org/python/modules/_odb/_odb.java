package org.python.modules._odb;

import org.python.core.*;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {

    protected static int timestamp = 0;
    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static LinkedList<OdbEvent> eventHistory = new LinkedList<>();
    protected static OdbFrame parent = null;

    protected static int currentFrame = -1;
    protected static int currentTimestamp = -1;


    public static void callEvent(PyFrame frame) {
        PyStringMap locals = (PyStringMap) frame.getLocals();
        Map<Object, PyObject> localMap = locals.getMap();

        parent = new OdbFrame(timestamp,
                frame.f_code.co_filename,
                frame.f_back.f_lineno,
                frame.f_code.co_name,
                parent,
                localMap);

        frames.push(parent);
        currentFrame++;

        eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.CALL));

        Py.writeMessage("TTD", frames.peek().toString());
        timestamp++;
        currentTimestamp++;
    }

    public static void returnEvent(PyFrame frame) {
        if (!frames.empty()) {
            Py.writeMessage("TTD", "Return from event: " + parent.toString());
            eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.RETURN));
            parent = parent.parent;
            timestamp++;
            currentTimestamp++;
        }
    }

    public static void lineEvent(PyFrame frame) {
        eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.LINE));
        timestamp++;
        currentTimestamp++;
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

    public static List<OdbEvent> getEvents() {
        return eventHistory;
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

    public static void step(){
        jump(currentTimestamp+1);
    }

    public static void rstep(){
        jump(currentTimestamp-1);
    }

    public static void jump(int n){
        if(n >= 0 && n < eventHistory.size()){
            currentTimestamp = n;
            currentFrame = frames.indexOf(getCurrentEvent().frame);
        }
    }

    public static void moveUpFrames() {
        OdbFrame frame = getCurrentFrame();
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
        OdbEvent event = null;
        for (int i = currentTimestamp; i < eventHistory.size(); i++){
            event = eventHistory.get(i);
            if(event.frame != null && event.frame.parent == frame){
                break;
            }
        }

        currentTimestamp = event.timestamp;
        currentFrame = frames.indexOf(event.frame);
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
