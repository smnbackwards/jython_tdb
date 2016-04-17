package org.python.modules._odb;

import org.python.core.*;

import java.util.Stack;

/**
 * Created by nms12 on 4/15/2016.
 */
public class _odb {

    protected static long timestamp = 0;
    protected static Stack<OdbFrame> frames = new Stack<>();
    protected static OdbFrame parent = null;
    protected static int currentFrame = -1;

    public static void callEvent(PyFrame frame) {
        parent = new OdbFrame(timestamp++, frame.f_code.co_filename, frame.f_back.f_lineno, frame.f_code.co_name, parent);
        frames.push(parent);
        currentFrame++;
        Py.writeMessage("TTD", frames.peek().toString());
    }

    public static void returnEvent(PyFrame frame) {
        if (!frames.empty()) {
            Py.writeMessage("TTD", "Return from event: " + parent.toString());
            parent = parent.parent;
        }
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
