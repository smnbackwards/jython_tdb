package org.python.modules._odb;

import com.google.common.collect.testing.*;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.features.MapFeature;
import com.google.common.collect.testing.testers.*;
import junit.framework.Assert;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.python.core.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.Assert.assertEquals;

/**
 * Created by nms12 on 5/3/2016.
 */
@RunWith(AllTests.class)

public class OdbMapTest {

    public static TestSuite suite() throws NoSuchMethodException {
        TestSuite suite = new TestSuite();

        suite.addTest(new JUnit4TestAdapter(AdditionalTests.class));
        //Tests with replaying off ie using the normal map
        suite.addTest(ConcurrentMapTests.suite(i -> new OdbMap<>(i), "Odb Map Tests"));
        //Timestamp is fixed, so values are assigned over
        suite.addTest(ConcurrentMapTests.suite(i -> new FixedTimeReplayOdbMap<>(i), "Odb Map Fixed Time Replay Tests"));
        //Timestamp increments after each operation
        suite.addTest(ConcurrentMapTests.suite(i -> new TestOdbMap<>(i), "Odb Map Replay Tests"));
        //Adds values to the map during creation and removes them again to populate the history of the backing map
        suite.addTest(ConcurrentMapTests.suite(i -> new TestOdbMapWithHistory(i), "Odb Map Replay Tests With History"));
        //The base tests to check that the backing list conforms to the same interface/tests
        suite.addTest(ConcurrentMapTests.suite(i -> new ConcurrentHashMap<>(i), "Concurrent Map Tests"));
        return suite;
    }

    public static class AdditionalTests {



        @Test
        public void testKeysetRemove() {
//            ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();
            _odb.replaying = true;
            Map<String,String> map = new OdbMap<String, String>(0);

            String value = "value";
            map.put("key", value);

            map.keySet().remove("key");
            assertEquals(0, map.size());
        }

        @Test
        public void testKeysetIteratorRemove() {
//            ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();
            _odb.replaying = true;
            Map<String,String> map = new OdbMap<String, String>(0);

            String value = "value";
            map.put("key", value);

            Iterator<String> iterator = map.keySet().iterator();
            iterator.next();
            iterator.remove();
            assertEquals(0, map.size());
        }

    }

    static class FixedTimeReplayOdbMap<K,V> extends OdbMap<K,V>{

        public FixedTimeReplayOdbMap(int capacity, float loadFactor, int concurrencyLevel) {
            super(capacity, loadFactor, concurrencyLevel);
        }

        public FixedTimeReplayOdbMap(int capacity) {
            super(capacity);
        }

        public FixedTimeReplayOdbMap(Map<K, V> map) {
            super(map);
        }

        @Override
        protected boolean isReplaying() {
            return true;
        }

        @Override
        protected int getTimestamp() {
            return 1;
        }
    }

    static class TestOdbMap<K,V> extends OdbMap<K,V>{

        public TestOdbMap(int capacity, float loadFactor, int concurrencyLevel) {
            super(capacity, loadFactor, concurrencyLevel);
        }

        public TestOdbMap(int capacity) {
            super(capacity);
        }

        public TestOdbMap(Map<K, V> map) {
            super(map);
        }

        int timestamp = 0;

        @Override
        protected boolean isReplaying() {
            return true;
        }

        @Override
        protected int getTimestamp() {
            return timestamp++;
        }
    }

    static class TestOdbMapWithHistory extends TestOdbMap<String, String>{

        public TestOdbMapWithHistory(int capacity, float loadFactor, int concurrencyLevel) {
            super(capacity, loadFactor, concurrencyLevel);
        }

        public TestOdbMapWithHistory(int capacity) {
            super(capacity);
        }

        public TestOdbMapWithHistory(Map<String, String> map) {
            super(map);
        }

        void initializeHistory(){
            Map<String,String> defaults = new HashMap<>();
            defaults.put("one", "January");
            defaults.put("two", "February");
            defaults.put("four", "April");
            defaults.put("five", "May");
            putAll(defaults);
            clear();
            put("one", "February");
            put("four", "4");
            remove("one");

            Iterator<Entry<String, String>> iterator = entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public static class ConcurrentMapTests extends TestCase {
        public static TestSuite suite(java.util.function.Function<Integer,Map<String,String>> mapGenerator, String name) throws NoSuchMethodException {
            return MapTestSuiteBuilder
                    .using(new TestStringMapGenerator() {

                        @Override
                        protected Map<String, String> create(Map.Entry<String, String>[] entries) {
                            Map<String, String> map = mapGenerator.apply(entries.length);
                            for (Map.Entry<String, String> e : entries) {
                                map.put(e.getKey(), e.getValue());
                            }
                            return map;
                        }
                    })
                    .named(name)
                    .withFeatures(
                            MapFeature.GENERAL_PURPOSE,
                            CollectionSize.ANY
                    )
                    .suppressing(CollectionAddAllTester.class.getMethod("testAddAll_unsupportedSomePresent"))
                    .suppressing(CollectionAddAllTester.class.getMethod("testAddAll_unsupportedNonePresent"))
                    .suppressing(CollectionAddTester.class.getMethod("testAdd_unsupportedNotPresent"))
                    .suppressing(CollectionIteratorTester.class.getMethod("testIterator_unknownOrderRemoveUnsupported"))
                    .createTestSuite();
        }
    }
}