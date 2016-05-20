package org.python.modules._odb;

/**
 * Created by nms12 on 5/18/2016.
 */
public class OdbLineEvent extends OdbEvent {
    public OdbLineEvent(int lineno, OdbFrame frame) {
        super(lineno, frame);
    }

    @Override
    public String event_type() {
        return "Line";
    }
}
