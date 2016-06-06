package org.python.modules;

import org.python.core.Py;
import org.python.core.PyLong;
import org.python.core.PyString;
import org.python.core.TdbTraceFunction;

/**
 * Created by dual- on 2/18/2016.
 */
public class _tdb {

    public static PyLong instruction_count() {
        return Py.newLong(TdbTraceFunction.getInstructionCount());
    }

    public static void reset_instruction_count() {
        TdbTraceFunction.resetInstructionCount();
    }

    public static PyLong call_depth() {
        return Py.newLong(TdbTraceFunction.getCallDepth());
    }

    public static PyLong get_last_call_instruction() {
        return Py.newLong(TdbTraceFunction.getLastCallInstructionCount());
    }

    public static PyLong get_previous_call_instruction() {
        return Py.newLong(TdbTraceFunction.getPreviousCallInstructionCount());
    }

    public static void set_stop_info(int stopIc, int stopDepth, String stopEvent) {
        TdbTraceFunction.stopIc = stopIc;
        TdbTraceFunction.stopDepth = stopDepth;
        TdbTraceFunction.stopEvent = stopEvent;
    }

    public static boolean get_redomode(){
        return TdbTraceFunction.redoMode;
    }

    public static void set_redomode(boolean redomode){
        TdbTraceFunction.redoMode = redomode;
    }

    public static PyString get_event(){
        return Py.newString(TdbTraceFunction.event);
    }

    public static PyString get_last_event(){
        return Py.newString(TdbTraceFunction.lastEvent);
    }

    public static void reset(){
        TdbTraceFunction.reset();
    }
}
