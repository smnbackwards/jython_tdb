package org.python.modules._odb;

import org.python.core.*;

import java.util.*;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {

    protected static long timestamp = 0;
    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static LinkedList<OdbEvent> eventHistory = new LinkedList<>();
    protected static OdbFrame parent = null;
    protected static int currentFrame = -1;


    public static void callEvent(PyFrame frame) {
        PyStringMap locals = (PyStringMap) frame.getLocals();
        Map<Object,PyObject> localMap = locals.getMap();

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
    }

    public static void returnEvent(PyFrame frame) {
        if (!frames.empty()) {
            Py.writeMessage("TTD", "Return from event: " + parent.toString());
            eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.RETURN));
            parent = parent.parent;
            timestamp++;
        }
    }

    public static void lineEvent(PyFrame frame){
        eventHistory.add(new OdbEvent(timestamp, frame.f_lineno, parent, OdbEvent.Type.LINE));
        timestamp++;
    }

    public static List<OdbEvent> getEvents(){
        return eventHistory;
    }

    public static OdbFrame getCurrentFrame() {
        return frames.elementAt(currentFrame);
    }

    public static void next() {
        if(currentFrame < frames.size()-1) {
            currentFrame++;
        } else {
        }
    }

    public static void previous() {
        if (currentFrame > 0) {
            currentFrame--;
        }
    }

}
