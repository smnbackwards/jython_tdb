package org.python.modules._odb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by nms12 on 4/28/2016.
 */
public class HistoryValueTest {

    @Test
    public void testGetTimestamp(){
        String a = "a";
        HistoryValue<String> v = new HistoryValue<>(0, a);

        assertEquals(0, v.getTimestamp());
    }

    @Test
    public void testGetValue(){
        String a = "a";
        HistoryValue<String> v = new HistoryValue<>(0, a);

        assertEquals(a, v.getValue());
    }
}
