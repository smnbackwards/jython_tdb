package org.python.modules._odb;

import org.junit.Test;
import org.python.core.PyString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by nms12 on 5/8/2016.
 */
public class HistoryValueListTest {
    @Test
    public void testConstructor() throws Exception {
        String a = "a";
        HistoryValueList<String> l = new HistoryValueList<>(0,a);
        assertEquals(a, l.getValue(0));
    }

    @Test
    public void testGetAfterTimestamp() throws Exception {
        String a = "a";
        HistoryValueList<String> l = new HistoryValueList<String>(0, a);
        assertEquals(a, l.getValue(10));
    }

    @Test
    public void testGetConsecutiveValues() throws Exception {
        String a = "a";
        String b = "b";
        String c = "c";
        HistoryValueList<String> l = new HistoryValueList<String>(0, a);
        l.insertValue(1, b);
        l.insertValue(2, c);
        assertNull(l.getValue(-1));
        assertEquals(a, l.getValue(0));
        assertEquals(b, l.getValue(1));
        assertEquals(c, l.getValue(2));
        assertEquals(c, l.getValue(3));
    }


    @Test
    public void testGetNonConsecutiveValues() throws Exception {
        String a = "a";
        String b = "b";
        String c = "c";
        HistoryValueList<String> l = new HistoryValueList<String>(0, a);
        l.insertValue(2, b);
        l.insertValue(5, c);
        assertNull(l.getValue(-1));
        assertEquals(a, l.getValue(0));
        assertEquals(a, l.getValue(1));
        assertEquals(b, l.getValue(2));
        assertEquals(b, l.getValue(3));
        assertEquals(b, l.getValue(4));
        assertEquals(c, l.getValue(5));
        assertEquals(c, l.getValue(6));
        assertEquals(c, l.getValue(7));
    }

    @Test
    public void testReplaceValue() throws Exception {
        String a = "a";
        String b0 = "b";
        HistoryValueList<String> l = new HistoryValueList<String>(0, a);
        l.insertValue(0, b0);
        assertEquals(b0, l.getValue(0));
    }

    @Test
    public void testGetBeforeValueExistsReturnsNull() throws Exception {
        String a = "a";
        HistoryValueList<String> l = new HistoryValueList<String>(1, a);
        assertNull(l.getValue(0));
    }
}
