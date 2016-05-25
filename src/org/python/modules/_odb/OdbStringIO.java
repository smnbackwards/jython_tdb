package org.python.modules._odb;

import org.python.core.OdbTraceFunction;
import org.python.core.PyObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nms12 on 5/17/2016.
 * Used by the Odb debugger in odb.py to store
 */
public class OdbStringIO {

    protected HistoryValueList<String> outputHistory = new HistoryValueList<>(-1, "");
    protected StringBuilder currentString = new StringBuilder();
    protected int currentTimestamp = -1;

    protected int getTimestamp() {
        return OdbTraceFunction.getCurrentTimestamp();
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

    public String getBetween(int startTimestamp, int endTimestamp) {
        if (startTimestamp == endTimestamp) {
            return get(startTimestamp);
        }

        if (startTimestamp > endTimestamp) {
            return null;
        }

        flushCurrentValue();
        HistoryValue<String> v;
        StringBuilder s = new StringBuilder();
        for (Iterator<HistoryValue<String>> iterator = outputHistory.values.iterator(); iterator.hasNext(); ) {
            v = iterator.next();
            if (v.getTimestamp() >= startTimestamp && v.getTimestamp() <= endTimestamp) {
                s.append(v.getValue());
            }
        }
        return s.length() > 0 ? s.toString() : null;
    }

    public List<String> getLastN(int timestamp, int n) {
        flushCurrentValue();
        HistoryValue<String> v;
        LinkedList<String> output = new LinkedList<>();
        for (Iterator<HistoryValue<String>> iterator = outputHistory.values.descendingIterator(); iterator.hasNext(); ) {
            v = iterator.next();
            if(v.getTimestamp() > timestamp || v.getTimestamp() == -1) {
                //move back to the desired timestamp
                continue;
            }

            if(n <= 0){
                break;
            }
            output.addFirst(v.getValue());
            n--;
        }
        return output;
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
