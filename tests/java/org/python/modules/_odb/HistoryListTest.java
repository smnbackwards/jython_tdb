package org.python.modules._odb;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

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
        assertEquals(a, list.get(0, 0));
        //and afterwards since it wasn't assigned over
        assertEquals(a, list.get(1, 0));
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
        assertEquals(a, list.get(0, 0));
        assertEquals(a, list.get(1, 0));
        assertEquals(a, list.get(2, 0));

        assertEquals(b, list.get(1, 1));
        assertEquals(b, list.get(2, 1));

        assertEquals(c, list.get(2, 2));
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
        assertEquals(a, list.get(0, 0));
        assertEquals(a, list.get(1, 0));
        assertEquals(a, list.get(2, 0));

        assertEquals(b, list.get(2, 1));

        assertEquals(c, list.get(1, 1));
        assertEquals(c, list.get(2, 2));
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
        assertEquals(a, list.get(0, 0));
        assertEquals(a, list.get(1, 0));
        assertEquals(a, list.get(2, 0));

        assertEquals(b, list.get(1, 1));
        assertEquals(b, list.get(2, 1));

        assertEquals(c, list.get(2, 2));
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

        assertEquals(a, list.get(0, 0));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));

        assertEquals(a, list.get(2, 0));
        assertEquals(b, list.get(2, 1));
        assertEquals(c, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(a, list.get(3, 1));
        assertEquals(b, list.get(3, 2));
        assertEquals(c, list.get(3, 3));
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

        assertEquals(a, list.get(0, 0));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));

        assertEquals(a, list.get(2, 0));
        assertEquals(b, list.get(2, 1));
        assertEquals(c, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(b, list.get(3, 1));
        assertEquals(b, list.get(3, 2));
        assertEquals(c, list.get(3, 3));

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

        assertEquals(a, list.get(0, 0));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));

        assertEquals(a, list.get(2, 0));
        assertEquals(b, list.get(2, 1));
        assertEquals(c, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(b, list.get(3, 1));
        assertEquals(c, list.get(3, 2));
        assertEquals(c, list.get(3, 3));
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

    @Test
    public void testRemoveFirstIndex() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, 0);

        assertEquals(b, list.get(1, 0));
        assertEquals(c, list.get(1, 1));

    }

    @Test
    public void testRemoveFirstObject() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, a);

        assertEquals(b, list.get(1, 0));
        assertEquals(c, list.get(1, 1));

    }

    @Test
    public void testRemoveMiddleIndex() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, 1);

        assertEquals(a, list.get(1, 0));
        assertEquals(c, list.get(1, 1));

    }

    @Test
    public void testRemoveMiddleObject() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, b);

        assertEquals(a, list.get(1, 0));
        assertEquals(c, list.get(1, 1));

    }

    @Test
    public void testRemoveLastIndex() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, 2);

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));

    }

    @Test
    public void testRemoveLastObject() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, c);

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));

    }

    @Test
    public void testRemoveDecreasesSize() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, c);

        assertEquals(3, list.size(0));
        assertEquals(2, list.size(1));

    }

    @Test
    public void testAddAfterRemoveFirst() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, a);

        list.add(2, d);

        list.add(3, 0, a);

        assertEquals(b, list.get(2, 0));
        assertEquals(c, list.get(2, 1));
        assertEquals(d, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(b, list.get(3, 1));
        assertEquals(c, list.get(3, 2));
        assertEquals(d, list.get(3, 3));

    }

    @Test
    public void testAddAfterRemoveMiddle() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, b);

        list.add(2, d);

        list.add(3, 1, b);


        assertEquals(a, list.get(2, 0));
        assertEquals(c, list.get(2, 1));
        assertEquals(d, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(b, list.get(3, 1));
        assertEquals(c, list.get(3, 2));
        assertEquals(d, list.get(3, 3));
    }

    @Test
    public void testAddAfterRemoveLast() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, c);

        list.add(2, d);

        list.add(3, 2, c);

        assertEquals(a, list.get(2, 0));
        assertEquals(b, list.get(2, 1));
        assertEquals(d, list.get(2, 2));

        assertEquals(a, list.get(3, 0));
        assertEquals(b, list.get(3, 1));
        assertEquals(c, list.get(3, 2));
        assertEquals(d, list.get(3, 3));
    }

    @Test
    public void testAddAll0Empty() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "A";

        list.addAll(1, 0, Arrays.asList(a));

        assertEquals(a, list.get(1, 0));
        assertEquals(1, list.size(1));
    }

    @Test
    public void testAddAll0SingleStart() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";

        list.add(0, a);
        list.addAll(1, 0, Arrays.asList(b));

        assertEquals(a, list.get(0, 0));
        assertEquals(1, list.size(0));

        assertEquals(b, list.get(1, 0));
        assertEquals(a, list.get(1, 1));
        assertEquals(2, list.size(1));
    }

    @Test
    public void testAddAll0SingleEnd() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";

        list.add(0, a);
        list.addAll(1, 1, Arrays.asList(b));

        assertEquals(a, list.get(0, 0));
        assertEquals(1, list.size(0));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));
        assertEquals(2, list.size(1));
    }

    @Test
    public void testAddAll0SingleMiddle() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, c);
        list.addAll(1, 1, Arrays.asList(b));

        assertEquals(a, list.get(0, 0));
        assertEquals(c, list.get(0, 1));
        assertEquals(2, list.size(0));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));
        assertEquals(c, list.get(1, 2));
        assertEquals(3, list.size(1));
    }

    @Test
    public void testAddAllEmpty() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a,b,c));

        assertEquals(a, list.get(0, 0));
        assertEquals(b, list.get(0, 1));
        assertEquals(c, list.get(0, 2));
        assertEquals(3, list.size(1));
    }

    @Test
    public void testAddAllEnd() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);
        list.addAll(1, Arrays.asList(c, b, a));

        assertEquals(a, list.get(1, 0));
        assertEquals(b, list.get(1, 1));
        assertEquals(c, list.get(1, 2));
        assertEquals(c, list.get(1, 3));
        assertEquals(b, list.get(1, 4));
        assertEquals(a, list.get(1, 5));
        assertEquals(6, list.size(1));
    }

    @Test
    public void testAddAllIndexStart() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";

        list.add(0, b);
        list.addAll(1, 0, Arrays.asList(b, a, b, a));

        assertEquals(b, list.get(1, 0));
        assertEquals(a, list.get(1, 1));
        assertEquals(b, list.get(1, 2));
        assertEquals(a, list.get(1, 3));
        assertEquals(b, list.get(1, 4));
        assertEquals(5, list.size(1));
    }

    @Test
    public void testAddAllAfterRemove() throws Exception
    {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(0, b);
        list.add(0, c);

        list.remove(1, 0);
        list.remove(1, 0);

        list.addAll(2, Arrays.asList(c, b, a));

        assertEquals(c, list.get(2, 0));
        assertEquals(c, list.get(2, 1));
        assertEquals(b, list.get(2, 2));
        assertEquals(a, list.get(2, 3));
        assertEquals(4, list.size(2));
    }

    @Test
    public void testSet() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a,b,c));

        list.set(1,0,c);
        list.set(2,1,a);
        list.set(3,2,b);

        assertEquals(a, list.get(0, 0));
        assertEquals(b, list.get(0, 1));
        assertEquals(c, list.get(0, 2));

        assertEquals(c, list.get(1, 0));
        assertEquals(b, list.get(1, 1));
        assertEquals(c, list.get(1, 2));

        assertEquals(c, list.get(2, 0));
        assertEquals(a, list.get(2, 1));
        assertEquals(c, list.get(2, 2));

        assertEquals(c, list.get(3, 0));
        assertEquals(a, list.get(3, 1));
        assertEquals(b, list.get(3, 2));

        assertEquals(3, list.size(0));
        assertEquals(3, list.size(1));
        assertEquals(3, list.size(2));
        assertEquals(3, list.size(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetOutOfBounds() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a,b,c));
        list.set(1,4,c);
    }

    @Test
    public void testUpdate() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a, b, a, b, a));
        list.update(1, Arrays.asList(c, b, c, b, c));
        list.update(2, Arrays.asList(b, b));

        assertEquals(c, list.get(1, 0));
        assertEquals(b, list.get(1, 1));
        assertEquals(c, list.get(1, 2));
        assertEquals(b, list.get(1, 3));
        assertEquals(c, list.get(1, 4));

        assertEquals(b, list.get(2, 0));
        assertEquals(b, list.get(2, 1));
    }

    @Test
    public void testClear() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a, b, c));
        list.clear(1);
        list.addAll(2, Arrays.asList(b, c, a));
        list.clear(3);

        assertEquals(3, list.size(0));
        assertEquals(0, list.size(1));
        assertEquals(3, list.size(2));
        assertEquals(0, list.size(3));

        try{
            list.get(1,0);
            fail();
        } catch (IndexOutOfBoundsException e){

        }
    }

    @Test
    public void testIndexOf() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.addAll(0, Arrays.asList(a, b, c, b, a));

        assertEquals(0, list.indexOf(0, a));
        assertEquals(1, list.indexOf(0, b));
        assertEquals(2, list.indexOf(0, c));
        assertEquals(-1, list.indexOf(0, d));
    }

    @Test
    public void testLastIndexOf() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.addAll(0, Arrays.asList(a, b, c, b, a));

        assertEquals(4, list.lastIndexOf(0, a));
        assertEquals(3, list.lastIndexOf(0, b));
        assertEquals(2, list.lastIndexOf(0, c));
        assertEquals(-1, list.lastIndexOf(0, d));
    }

    @Test
    public void testContains() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.addAll(0, Arrays.asList(a, b, c, b, a));

        assertTrue(list.contains(0, a));
        assertTrue(list.contains(0, b));
        assertTrue(list.contains(0, c));
        assertFalse(list.contains(0,d));
    }

    @Test
    public void testContainsEmpty() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";

        assertFalse(list.contains(0, a));
    }

    @Test
    public void testContainsEmptyAfterRemove() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a, b, c, b, a));
        list.clear(1);

        assertFalse(list.contains(1, a));
        assertFalse(list.contains(1, b));
        assertFalse(list.contains(1, c));
    }

    @Test
    public void testContainsAll() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        list.addAll(0, Arrays.asList(a, b, c, b, a));

        assertTrue(list.containsAll(0, Arrays.asList(a,b,c)));
        assertFalse(list.containsAll(0,Arrays.asList(a,b,c,d)));
    }

    @Test
    public void testContainsAllAfterClear() throws Exception{
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.addAll(0, Arrays.asList(a, b, c, b, a));
        list.clear(1);

        assertFalse(list.containsAll(1, Arrays.asList(a,b,c)));
    }

    @Test
    public void testIteratorTraversal() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);
        list.remove(3, a);
        list.set(4, 1, b);
        list.clear(5);

        testIterator(0, list, new String[]{a});
        testIterator(1, list, new String[]{a,b});
        testIterator(2, list, new String[]{a,b,c});
        testIterator(3, list, new String[]{b,c});
        testIterator(4, list, new String[]{b,b});
        testIterator(5, list, new String[]{});
    }

    private void testIterator(int timestamp, HistoryList<String> list, String[] expected){
        Iterator<String> it = list.iterator(timestamp);
        for (String s: expected) {
            assertEquals(s, it.next());
        }
        assertFalse(it.hasNext());

    }

    @Test
    public void testListIteratorTraversal() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);
        list.remove(3, a);
        list.set(4, 1, b);
        list.clear(5);

        testListIterator(0, list, new String[]{a});
        testListIterator(1, list, new String[]{a,b});
        testListIterator(2, list, new String[]{a,b,c});
        testListIterator(3, list, new String[]{b,c});
        testListIterator(4, list, new String[]{b,b});
        testListIterator(5, list, new String[]{});
    }

    private void testListIterator(int timestamp, HistoryList<String> list, String[] expected){
        ListIterator<String> it = list.listIterator(timestamp);
        if(expected.length == 0){
            assertFalse(it.hasPrevious());
            assertFalse(it.hasNext());
            return;
        }

        assertFalse(it.hasPrevious());
        assertTrue(it.hasNext());
        for (int i=0; i<expected.length; i++) {
            assertEquals(i, it.nextIndex());
            assertEquals(expected[i], it.next());
        }
        assertTrue(it.hasPrevious());
        assertFalse(it.hasNext());
        for (int i = expected.length-1; i >= 0; i--) {
            assertEquals(i, it.previousIndex());
            assertEquals(expected[i], it.previous());
        }
        assertFalse(it.hasPrevious());

    }

    @Test
    public void testToArray() throws Exception {
        HistoryList<String> list = new HistoryList<>();

        String a = "a";
        String b = "b";
        String c = "c";

        list.add(0, a);
        list.add(1, b);
        list.add(2, c);
        list.remove(3, a);
        list.set(4, 1, b);
        list.clear(5);


        testArrayEquals(new String[]{a}, list.toArray(0));
        testArrayEquals(new String[]{a,b}, list.toArray(1));
        testArrayEquals(new String[]{a,b,c}, list.toArray(2));
        testArrayEquals(new String[]{b,c}, list.toArray(3));
        testArrayEquals(new String[]{b,b}, list.toArray(4));
        testArrayEquals(new String[]{}, list.toArray(5));

    }

    private void testArrayEquals(Object[] expected, Object[] actual){
        if(expected == null && actual == null){
            return;
        }
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}
