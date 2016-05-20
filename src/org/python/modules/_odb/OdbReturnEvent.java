package org.python.modules._odb;

/**
 * Created by nms12 on 5/18/2016.
 */
public class OdbReturnEvent extends OdbEvent {

    public OdbReturnEvent(int lineno, OdbFrame frame) {
        super(lineno, frame);
    }

    @Override
    public String event_type() {
        return "Return";
    }
}
