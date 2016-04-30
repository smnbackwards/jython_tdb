package org.python.modules._odb;

import com.google.common.collect.testing.*;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.ListFeature;
import com.google.common.collect.testing.testers.ListRetainAllTester;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.junit.runners.Suite;
import org.python.antlr.op.Add;
import org.python.core.*;

import java.util.*;

@RunWith(AllTests.class)
public class OdbListTest {

    public static TestSuite suite() throws NoSuchMethodException {
        TestSuite suite = new TestSuite();

        suite.addTest(new JUnit4TestAdapter(AdditionalTests.class));
        suite.addTest(GuavaStringListTests.suite());
        suite.addTest(GuavaPyObjectListTests.suite());
        return suite;
    }

    public static class AdditionalTests {

        @Test
        public void testFoo() {

        }

    }

    public static class GuavaStringListTests extends TestCase {
        public static TestSuite suite() throws NoSuchMethodException {
            return ListTestSuiteBuilder
                    .using(new TestStringListGenerator() {

                        @Override
                        protected List<String> create(String[] elements) {
                            return new OdbList<>(Arrays.asList(elements));
                        }


                    })
                    .named("OdbList String tests")
                    .withFeatures(
                            ListFeature.GENERAL_PURPOSE,
                            ListFeature.SUPPORTS_ADD_WITH_INDEX,
                            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
                            ListFeature.SUPPORTS_SET,
                            CollectionFeature.SUPPORTS_ADD,
                            CollectionFeature.SUPPORTS_REMOVE,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionFeature.GENERAL_PURPOSE,
                            CollectionSize.ANY
                    )
                    //Broken dependency in this test
                    .suppressing(ListRetainAllTester.class.getMethod("testRetainAll_countIgnored")
                    ).createTestSuite();
        }
    }

    public static class GuavaPyObjectListTests extends TestCase {
        public static TestSuite suite() throws NoSuchMethodException {
            return ListTestSuiteBuilder
                    .using(new TestListGenerator<PyObject>() {

                        @Override
                        public SampleElements<PyObject> samples() {
                            return new SampleElements<>(
                                    new PyString("this is a pystring"),
                                    new PyInteger(991),
                                    new PyInteger(992),
                                    new PyLong(993),
                                    new PyFloat(999.4)
                            );
                        }

                        @Override
                        public List<PyObject> create(Object... elements) {
                            PyObject[] array = new PyObject[elements.length];
                            int i = 0;
                            for (Object e : elements) {
                                array[i++] = (PyObject) e;
                            }
                            return new OdbList<>(Arrays.asList(array));
                        }

                        @Override
                        public PyObject[] createArray(int length) {
                            return new PyObject[length];
                        }

                        @Override
                        public Iterable<PyObject> order(List<PyObject> insertionOrder) {
                            return insertionOrder;
                        }

                    })
                    .named("OdbList PyObject tests")
                    .withFeatures(
                            ListFeature.GENERAL_PURPOSE,
                            ListFeature.SUPPORTS_ADD_WITH_INDEX,
                            ListFeature.SUPPORTS_REMOVE_WITH_INDEX,
                            ListFeature.SUPPORTS_SET,
                            CollectionFeature.SUPPORTS_ADD,
                            CollectionFeature.SUPPORTS_REMOVE,
                            CollectionFeature.SUPPORTS_ITERATOR_REMOVE,
                            CollectionFeature.ALLOWS_NULL_VALUES,
                            CollectionFeature.GENERAL_PURPOSE,
                            CollectionSize.ANY
                    )
                    //Broken dependency in this test
                    .suppressing(ListRetainAllTester.class.getMethod("testRetainAll_countIgnored")
                    ).createTestSuite();
        }
    }
}