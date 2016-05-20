package org.python.modules._odb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nms12 on 5/19/2016.
 */
public class OdbEventTest {

    @Test
    public void testType() {
        assertEquals(OdbEvent.EVENT_TYPE.LINE, OdbEvent.decodeEventType(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.LINE)));
        assertEquals(OdbEvent.EVENT_TYPE.CALL, OdbEvent.decodeEventType(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.CALL)));
        assertEquals(OdbEvent.EVENT_TYPE.RETURN, OdbEvent.decodeEventType(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.RETURN)));
        assertEquals(OdbEvent.EVENT_TYPE.EXCEPTION, OdbEvent.decodeEventType(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.EXCEPTION)));
    }

    @Test
    public void testLine() {
        assertEquals(0, OdbEvent.decodeEventLineno(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.LINE)));
        assertEquals(1, OdbEvent.decodeEventLineno(OdbEvent.createEvent(1, 0, OdbEvent.EVENT_TYPE.LINE)));
        assertEquals(1 << 29, OdbEvent.decodeEventLineno(OdbEvent.createEvent(1 << 29, 0, OdbEvent.EVENT_TYPE.LINE)));
    }

    @Test
    public void testFrameId() {
        assertEquals(0, OdbEvent.decodeEventFrameId(OdbEvent.createEvent(0, 0, OdbEvent.EVENT_TYPE.LINE)));
        assertEquals(1, OdbEvent.decodeEventFrameId(OdbEvent.createEvent(0, 1, OdbEvent.EVENT_TYPE.LINE)));
        assertEquals(2 ^ 32 - 1, OdbEvent.decodeEventFrameId(OdbEvent.createEvent(0, 2 ^ 32 - 1, OdbEvent.EVENT_TYPE.LINE)));
    }

    @Test
    public void test(){

        int lineno = 0;
        int frameid = 0;
        OdbEvent.EVENT_TYPE type = OdbEvent.EVENT_TYPE.RETURN;

        long event = OdbEvent.createEvent(lineno, frameid, type);
        assertEquals(type, OdbEvent.decodeEventType(event));
        assertEquals(lineno, OdbEvent.decodeEventLineno(event));
        assertEquals(frameid, OdbEvent.decodeEventFrameId(event));
    }

    @Test
    public void testCombined() {

        int[] linenos = {0, 1, 1 << 29, 2 ^ 31 - 1};
        int[] frameids = {0, 1, 1 << 31, 2 ^ 32 - 1};

        for (OdbEvent.EVENT_TYPE type : OdbEvent.EVENT_TYPE.values()) {
            for (int lineno : linenos) {
                for (int frameid : frameids) {
                    System.out.println("Testing " + type.toString() +" " + lineno +" " + frameid);
                    long event = OdbEvent.createEvent(lineno, frameid, type);
                    assertEquals(type, OdbEvent.decodeEventType(event));
                    assertEquals(lineno, OdbEvent.decodeEventLineno(event));
                    assertEquals(frameid, OdbEvent.decodeEventFrameId(event));

                }

            }

        }
    }
}
