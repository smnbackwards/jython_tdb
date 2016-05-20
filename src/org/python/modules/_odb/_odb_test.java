package org.python.modules._odb;

import org.python.core.OdbTraceFunction;
import org.python.core.PyObject;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedSet;
import org.python.expose.ExposedType;

/**
 * Created by nms12 on 5/20/2016.
 * A test version of odb which allows the test to mock the main features of odb
 * without needing to create a full debugger instance
 */
@ExposedType
public class _odb_test extends PyObject {

    @ExposedGet(name = "currentTimestamp")
    public int getCurrentTimestamp(){
        return OdbTraceFunction.getCurrentTimestamp();
    }

    @ExposedSet(name = "currentTimestamp")
    public void setCurrentTimestamp(int timestamp){
        OdbTraceFunction.setCurrentTimestamp(timestamp);
    }

    @ExposedGet(name = "enabled")
    public boolean getEnabled(){
        return OdbTraceFunction.isEnabled();
    }

    @ExposedSet(name = "enabled")
    public void setEnabled(boolean enabled){
        OdbTraceFunction.setEnabled(enabled);
    }

    @ExposedGet(name = "replaying")
    public boolean getReplaying(){
        return OdbTraceFunction.isReplaying();
    }

    @ExposedSet(name = "replaying")
    public void setReplaying(boolean replaying){
        OdbTraceFunction.setReplaying(replaying);
    }

}
