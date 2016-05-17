package org.python.modules._odb;

import org.python.core.PyObject;

/**
 * Created by nms12 on 5/17/2016.
 * Used by the Odb debugger in odb.py to store
 */
public class OdbStringIO {

    protected HistoryValueList<String> outputHistory = new HistoryValueList<>(-1, null);
    protected StringBuilder currentString = new StringBuilder();
    protected int currentTimestamp = -1;

    protected int getTimestamp() {
        return _odb.getCurrentTimestamp();
    }

    //required by python spec, but not implemented in Jython so we ignore it too
    public boolean softspace = false;

    protected void flushCurrentValue() {
        int timestamp = getTimestamp();
        if (currentTimestamp != timestamp) {
            if (currentString.length() > 0) {
                outputHistory.insertValue(currentTimestamp, currentString.toString());
            }
            currentString.setLength(0);
            currentTimestamp = timestamp;
        }
    }

    public void write(PyObject obj) {
        write(obj.toString());
    }

    public void write(String s) {
        flushCurrentValue();
        currentString.append(s);
    }

    public String get(int timestamp) {
        flushCurrentValue();
        HistoryValue<String> historyValue = outputHistory.getHistoryValue(timestamp);
        return historyValue.getTimestamp() == timestamp ? historyValue.getValue() : null;
    }

    public String getvalue() {
        flushCurrentValue();
        StringBuilder s = new StringBuilder();
        for (HistoryValue h : outputHistory.values) {
            s.append(h.getValue());
        }
        return s.toString();
    }
}
