package org.python.modules._odb;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nms12 on 4/28/2016.
 */
public class HistoryListTest {

    @Test
    public void testAddSingle() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";

        list.add(0, a);

        //value is a at timepoint
        assertEquals(a, list.get(0,0));
        //and afterwards since it wasn't assigned over
        assertEquals(a, list.get(1,0));
    }

    @Test
    public void testAdd() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);

        //a exists at all times
        assertEquals(a, list.get(0,0));
        assertEquals(a, list.get(1,0));
        assertEquals(a, list.get(2,0));

        assertEquals(b, list.get(1,1));
        assertEquals(b, list.get(2,1));

        assertEquals(c, list.get(2,2));
    }

    @Test
    public void testAddReplace() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, c);
        list.add(2, 1, b);

        //a exists at all times
        assertEquals(a, list.get(0,0));
        assertEquals(a, list.get(1,0));
        assertEquals(a, list.get(2,0));

        assertEquals(b, list.get(2,1));

        assertEquals(c, list.get(1,1));
        assertEquals(c, list.get(2,2));
    }

    @Test
    public void testAddWithIndexAll() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, 0, a);
        list.add(1, 1, b);
        list.add(2, 2, c);

        //a exists at all times
        assertEquals(a, list.get(0,0));
        assertEquals(a, list.get(1,0));
        assertEquals(a, list.get(2,0));

        assertEquals(b, list.get(1,1));
        assertEquals(b, list.get(2,1));

        assertEquals(c, list.get(2,2));
    }

    @Test
    public void testAddIndexStart() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);

        list.add(3, 0, a);

        assertEquals(a, list.get(0,0));

        assertEquals(a, list.get(1,0));
        assertEquals(b, list.get(1,1));

        assertEquals(a, list.get(2,0));
        assertEquals(b, list.get(2,1));
        assertEquals(c, list.get(2,2));

        assertEquals(a, list.get(3,0));
        assertEquals(a, list.get(3,1));
        assertEquals(b, list.get(3,2));
        assertEquals(c, list.get(3,3));
    }

    @Test
    public void testAddIndexMiddle() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);

        list.add(3, 1, b);

        assertEquals(a, list.get(0,0));

        assertEquals(a, list.get(1,0));
        assertEquals(b, list.get(1,1));

        assertEquals(a, list.get(2,0));
        assertEquals(b, list.get(2,1));
        assertEquals(c, list.get(2,2));

        assertEquals(a, list.get(3,0));
        assertEquals(b, list.get(3,1));
        assertEquals(b, list.get(3,2));
        assertEquals(c, list.get(3,3));

    }

    @Test
    public void testAddIndexEnd() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);

        list.add(3, 2, c);

        assertEquals(a, list.get(0,0));

        assertEquals(a, list.get(1,0));
        assertEquals(b, list.get(1,1));

        assertEquals(a, list.get(2,0));
        assertEquals(b, list.get(2,1));
        assertEquals(c, list.get(2,2));

        assertEquals(a, list.get(3,0));
        assertEquals(b, list.get(3,1));
        assertEquals(c, list.get(3,2));
        assertEquals(c, list.get(3,3));
    }


    @Test
    public void testSizeIncrementsOnAdd() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, c);
        list.add(2, 1, b);

        assertEquals(0, list.size(-1));
        assertEquals(1, list.size(0));
        assertEquals(2, list.size(1));
        assertEquals(3, list.size(2));
        assertEquals(3, list.size(3));
    }


}
