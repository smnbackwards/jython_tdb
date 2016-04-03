import imp
import sys
import os
import unittest
import subprocess
import textwrap
from tdb import controller, tpdb
from test import test_support


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
        sys.stdin = _FakeInput(self.input)

    def __exit__(self, *exc):
        sys.stdin = self.real_stdin


class TestTdb(tpdb.Pdb):
    def __init__(self, skip=None):
        tpdb.Pdb.__init__(self, skip=skip)
        self.instructionsStoppedAt = []

    def interaction(self, frame, traceback):
        if tpdb.Pdb.interaction(self, frame, traceback):
            print "stopping at", self.get_ic()
            self.instructionsStoppedAt.append(self.get_ic())
            return True
        else:
            return False


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


def test_main():
    test_support.verbose = 1
    test_support.run_unittest(PdbTestCase)


if __name__ == '__main__':
    test_main()
