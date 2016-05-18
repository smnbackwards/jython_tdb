package org.python.modules._odb;

/**
 * Created by nms12 on 5/18/2016.
 */
public class OdbLineEvent extends OdbEvent {
    public OdbLineEvent(int timestamp, int lineno, OdbFrame frame) {
        super(timestamp, lineno, frame);
    }

    @Override
    public String event_type() {
        return "Line";
    }
}
