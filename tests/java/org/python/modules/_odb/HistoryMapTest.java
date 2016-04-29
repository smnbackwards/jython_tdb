package org.python.modules._odb;

import org.junit.Test;
import org.python.core.PyObject;
import org.python.core.PyString;

import static org.junit.Assert.*;

/**
 * Created by nms12 on 4/25/2016.
 */
public class HistoryMapTest {

    @Test
    public void getReturnsPutValue() throws Exception {
        HistoryMap<String> map = new HistoryMap<>();

        PyString a = new PyString("A");
        map.put(0, "a", a);
        assertEquals(a, map.get(0, "a"));
    }

    @Test
    public void getBeforePutReturnsNull() throws Exception {
        HistoryMap<String> map = new HistoryMap<>();

        PyString a = new PyString("A");
        map.put(1, "a", a);
        assertNull(map.get(0, "a"));
    }

    @Test
    public void putReplacesValueWithSameTimestamp() throws Exception {
        HistoryMap<String> map = new HistoryMap<>();

        PyString a = new PyString("A");
        PyString b = new PyString("B");
        map.put(0, "a", a);
        map.put(0, "a", b);
        assertEquals(b, map.get(0, "a"));
    }

    @Test
    public void getAfterTimestampReturnsLastValue() throws Exception {
        HistoryMap<String> map = new HistoryMap<>();

        PyString a = new PyString("A");
        map.put(0, "a", a);
        assertEquals(a, map.get(1, "a"));
    }

    @Test
    public void getAfterTimestampReturnsLastValueMiddle() throws Exception {
        HistoryMap<String> map = new HistoryMap<>();

        PyString a = new PyString("A");
        PyString b = new PyString("B");
        map.put(0, "a", a);
        map.put(2, "a", b);
        assertEquals(a, map.get(1, "a"));
    }
}