package org.python.modules._odb;

import org.junit.Test;
import org.python.core.PyString;

import static org.junit.Assert.*;

/**
 * Created by nms12 on 4/25/2016.
 */
public class LocalValueListTest {

    @Test
    public void testConstructor() throws Exception {
        PyString a = new PyString("a");
        LocalValueList l = new LocalValueList(0, a);
        assertEquals(a, l.getValue(0));
    }

    @Test
    public void testGetAfterTimestamp() throws Exception {
        PyString a = new PyString("a");
        LocalValueList l = new LocalValueList(0, a);
        assertEquals(a, l.getValue(10));
    }

    @Test
    public void testGetConsecutiveValues() throws Exception {
        PyString a = new PyString("a");
        PyString b = new PyString("b");
        PyString c = new PyString("c");
        LocalValueList l = new LocalValueList(0, a);
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
        PyString a = new PyString("a");
        PyString b = new PyString("b");
        PyString c = new PyString("c");
        LocalValueList l = new LocalValueList(0, a);
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
        PyString a = new PyString("a");
        PyString b0 = new PyString("b");
        LocalValueList l = new LocalValueList(0, a);
        l.insertValue(0, b0);
        assertEquals(b0, l.getValue(0));
    }

    @Test
    public void testGetBeforeValueExistsReturnsNull() throws Exception {
        PyString a = new PyString("a");
        LocalValueList l = new LocalValueList(1, a);
        assertNull(l.getValue(0));
    }
}