import sys
import unittest
from tdb import commandlinecontroller, tpdb, tbdb
from test import test_support


# tbdb.debug_enabled = 1

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
        self.finalinstructioncount = -1
        # don't restart debugging
        self._user_requested_quit = 1

    def interaction(self, frame, traceback):
        if not self.redomode:
            self.instructionsStoppedAt.append(self.get_ic())
            tpdb.Pdb.interaction(self, frame, traceback)

    def on_end(self):
        tpdb.Pdb.on_end(self)
        self.finalinstructioncount = self.get_ic()


class PdbTestCase(unittest.TestCase):
    def test_step_all(self):
        with TestInput(['step' for x in range(0, 22)] + ['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual(range(0, 22 + 1), debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_next_all(self):
        with TestInput(['next'] * 3 + ['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([0, 1, 21, 22], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_continue_all(self):
        with TestInput(['continue']):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([0], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_restart(self):
        with TestInput(['step', 'restart', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger,'examples/fib.py')
            self.assertSequenceEqual([0,1,0], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_next(self):
        with TestInput([
            'step',
            'step',
            'step',
            'step',
            'next',
            'next',
            'step',
            'step',
            'continue',
        ]):
            debugger = TestTdb()
            debugger._runscript('examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 4, 16, 20, 21, 22], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_break_line(self):
        with TestInput([
            'break 6',
            'continue',
            'continue'
        ]):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 22], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_break_function(self):
        with TestInput([
            'break 1',
            'step',
            'continue' # bp never gets hit, we can't break on a function?
        ]):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_break_function_call(self):
        with TestInput([
            'break 5',
            'continue',
            'continue' # bp doesn't get hit again
        ]):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_break_function_return(self):
        with TestInput([
            'break 3',
            'continue',
            'continue',# fib(1)
            'continue',# fib(0)
            'continue',# fib(1)
        ]):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 10,14,19], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rstep_one(self):
        with TestInput(['step', 'rstep', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 0], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rstep_two(self):
        with TestInput(['step', 'step', 'rstep', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rstep_out_of_call(self):
        with TestInput(['step', 'step', 'step', 'rstep', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 2], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rstep_arg(self):
        with TestInput(['step', 'step', 'step', 'rstep 1', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rreturn_after_return(self):
        with TestInput(['step'] * 2 + ['return', 'rreturn', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 21, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rreturn_depth_1(self):
        with TestInput(['step'] * 2 + ['rreturn', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

        with TestInput(['step'] * 3 + ['rreturn', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

        with TestInput(['step'] * 4 + ['rreturn', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 4, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rreturn_depth_2(self):
        with TestInput(['step'] * 7 + ['rreturn', 'rreturn', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 4, 5, 6, 7, 4, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rnext_line(self):
        with TestInput(['step', 'rnext', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rnext_call(self):
        with TestInput(['step'] * 2 + ['rnext', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 1], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rnext_return(self):
        with TestInput(['step'] * 4 + ['next', 'rnext', 'continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            self.assertSequenceEqual([0, 1, 2, 3, 4, 16, 4], debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)

    def test_rstep_all(self):
        with TestInput(['step' for x in range(1, 22)] + ['rstep' for x in range(1, 22)] + ['continue']):
            debugger = TestTdb()
            tpdb.mainloop(debugger, 'examples/fib.py')
            # Instructions 0-21 and then back down 20-0
            self.assertSequenceEqual(range(22) + range(20, -1, -1), debugger.instructionsStoppedAt)
            self.assertEqual(24, debugger.finalinstructioncount)


def test_main():
    test_support.verbose = 1
    test_support.run_unittest(PdbTestCase)


if __name__ == '__main__':
    test_main()
