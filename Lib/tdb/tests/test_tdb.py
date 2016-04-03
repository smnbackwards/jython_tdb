import imp
import sys
import os
import unittest
import subprocess
import textwrap
from tdb import commandlinecontroller, tpdb
from test import test_support


class NullDevice():
    def write(self, s):
        pass


class _FakeInput:
    """
    A fake input stream for pdb's interactive debugger.  Whenever a
    line is read, print it (to simulate the user typing it), and then
    return it.  The set of lines to return is specified in the
    constructor; they should not have trailing newlines.
    """

    def __init__(self, lines):
        self.lines = lines

    def readline(self):
        line = self.lines.pop(0)
        print line
        return line + '\n'


class TestInput(object):
    """Context manager that makes testing Pdb in doctests easier."""

    def __init__(self, input):
        self.input = input

    def __enter__(self):
        self.real_stdin = sys.stdin
        self.real_stdout = sys.stdout
        sys.stdin = _FakeInput(self.input)
        sys.stdout = NullDevice()

    def __exit__(self, *exc):
        sys.stdin = self.real_stdin
        sys.stdout = self.real_stdout


class TestTdb(tpdb.Pdb):
    def __init__(self, skip=None):
        # controller must be set here in order for stdout to be swallowed
        tpdb.Pdb.__init__(self, controller=commandlinecontroller.CommandLineController(), skip=skip)
        self.instructionsStoppedAt = []
        self._user_requested_quit = 1

    def interaction(self, frame, traceback):
        if not self.redomode:
            self.instructionsStoppedAt.append(self.get_ic())
            tpdb.Pdb.interaction(self, frame, traceback)


class PdbTestCase(unittest.TestCase):
    def test_a(self):
        with TestInput(["step", "s", "s", "continue", "quit"]):
            debugger = TestTdb()
            debugger._runscript('examples/tracefile.py')
            self.assertSequenceEqual([4L, 5L, 6L, 7L], debugger.instructionsStoppedAt)

    def test_b(self):
        with TestInput(["continue"]):
            debugger = TestTdb()
            debugger._runscript('examples/tracefile.py')
            self.assertSequenceEqual([4L], debugger.instructionsStoppedAt)

    def test_step_all(self):
        with TestInput(["step" for x in range(4, 26)] + ['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual(range(4, 26 + 1), debugger.instructionsStoppedAt)

    def test_next_all(self):
        with TestInput(['next'] * 3 + ['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([4, 5, 25, 26], debugger.instructionsStoppedAt)

    def test_continue_all(self):
        with TestInput(['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([4], debugger.instructionsStoppedAt)

    def test_next(self):
        with TestInput([
            "step",
            "step",
            "step",
            "step",
            "next",
            "next",
            "step",
            "step",
            "continue",
        ]):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 8, 20, 24, 25, 26], debugger.instructionsStoppedAt)

    def test_rstep_one(self):
        with TestInput(["step", "rstep", 'continue', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 4], debugger.instructionsStoppedAt)

    def test_rstep_two(self):
        with TestInput(["step", "step", "rstep", 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 5], debugger.instructionsStoppedAt)

    def test_rstep_out_of_call(self):
        with TestInput(["step", "step", "step", "rstep", 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 6], debugger.instructionsStoppedAt)

    def test_rstep_arg(self):
        with TestInput(['step', 'step', 'step', 'rstep 5', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 5], debugger.instructionsStoppedAt)

    def test_rreturn_after_return(self):
        with TestInput(['step'] * 2 + ['return', 'rreturn', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 25, 5], debugger.instructionsStoppedAt)

    def test_rreturn_depth_1(self):
        with TestInput(['step'] * 2 + ['rreturn', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 5], debugger.instructionsStoppedAt)
        with TestInput(['step'] * 3 + ['rreturn', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 5], debugger.instructionsStoppedAt)
        with TestInput(['step'] * 4 + ['rreturn', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 8, 5], debugger.instructionsStoppedAt)

    def test_rreturn_depth_2(self):
        with TestInput(['step'] * 7 + ['rreturn', 'rreturn', 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 8, 9, 10, 11, 8, 5], debugger.instructionsStoppedAt)

    def test_rnext_line(self):
        with TestInput(["step", "rnext", 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 4], debugger.instructionsStoppedAt)

    def test_rnext_call(self):
        with TestInput(["step"] * 2 + ["rnext", 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 5], debugger.instructionsStoppedAt)

    def test_rnext_return(self):
        with TestInput(["step"] * 4 + ["next", "rnext", 'quit']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([4, 5, 6, 7, 8, 20, 8], debugger.instructionsStoppedAt)

    def test_rstep_all(self):
        with TestInput(["step" for x in range(4, 26)] + ["rstep" for x in range(4, 26)] + ['continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual(range(4, 26 + 1) + range(25, 4 - 1, -1), debugger.instructionsStoppedAt)


def test_main():
    test_support.verbose = 1
    test_support.run_unittest(PdbTestCase)


if __name__ == '__main__':
    test_main()
