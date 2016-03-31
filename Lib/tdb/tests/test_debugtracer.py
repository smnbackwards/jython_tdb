import os
import unittest

from tdb.debugtracer import DebugTracer


class TestTracer(DebugTracer):
    def __init__(self, skip=[]):
        DebugTracer.__init__(self, skip=skip)
        self.linenos = []
        self.dispatched_calls = 0

    def user_line(self, frame):
        if self._wait_for_mainpyfile:
            if (self.mainpyfile != frame.f_code.co_filename or frame.f_lineno<= 0):
                return
            self._wait_for_mainpyfile = 0
        self.linenos.append(frame.f_lineno)

    def user_call(self, frame, argument_list):
        if self._wait_for_mainpyfile:
            return
        self.dispatched_calls += 1

    def runscript(self, filename):
        self._wait_for_mainpyfile = 1
        self.mainpyfile = os.path.abspath(filename)
        with open(filename, "rb") as fp:
            statement = "exec(compile(%r, %r, 'exec'))" % \
                        (fp.read(), self.mainpyfile)
        self.run(statement)


class TracerTestCase(unittest.TestCase):
    def runTest(self):
        tracer = TestTracer()
        tracer.runscript('tracefile.py')
        self.assertEqual(tracer.linenos, [2,4,6,9,7])

if __name__ == '__main__':
    unittest.main()