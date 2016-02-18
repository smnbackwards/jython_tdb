package org.python.modules;

import org.python.core.Py;
import org.python.core.PyLong;
import org.python.core.TdbTraceFunction;

/**
 * Created by dual- on 2/18/2016.
 */
public class _tdb {

    public static PyLong instruction_count(){
        return Py.newLong(TdbTraceFunction.getInstructionCount());
    }

    public static void reset_instruction_count(){
        TdbTraceFunction.resetInstructionCount();
    }
}
