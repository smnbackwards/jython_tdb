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
        return self

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
    '''
    The following graph shows the call depth and line number for each instruction count
    in the fib.py program
    When Tdb is first run, it will display the following prompts
            > ...\fib.py(1)<module>()
            -> def fib(n):
            (Tdb)<0>
        The first line indicates that we have stopped on line 1 of fib.py
        The third line indicates that the instruction count is 0

    Examples:
        'next'      at ic 4 -> 16 (see test_next)
        'return'    at ic 1 -> 21 (see test_return)


    ic: 0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19  20  21  22  23
    ----------------------------------------------------------------------------------------------------
    0  |                                                                                            6
    1  |1   5                                                                               4   6
    2  |        1   2   4                                               4               3
    3  |                    1   2   4               3               3       1   2   3
    4  |                                1   2   3       1   2   3
    '''

    def _test(self, filename, commands, instructions=None, total_instructions=-1, commands_remaining=-1):
        with TestInput(commands) as ti:
            debugger = TestTdb()
            tpdb.mainloop(debugger, filename)
            if instructions:
                self.assertSequenceEqual(instructions, debugger.instructionsStoppedAt)
            if total_instructions >= 0:
                self.assertEqual(total_instructions, debugger.finalinstructioncount)
            if commands_remaining >= 0:
                self.assertEqual(0, len(ti.input))

    def _test_fib(self, commands, instructions, total_instructions=24, commands_remaining=0):
        self._test('examples/fib.py', commands, instructions, total_instructions, commands_remaining)

    def test_step_all(self):
        self._test_fib(['step' for x in range(0, 22)] + ['continue'],
                       range(0, 22 + 1))

    def test_next_all(self):
        self._test_fib(['next'] * 3 + ['continue'],
                       [0, 1, 21, 22])

    def test_continue_all(self):
        self._test_fib(['continue'], [0])

    def test_restart(self):
        self._test_fib(['step', 'restart', 'continue'], [0, 1, 0])

    def test_next(self):
        self._test_fib([
            'step',
            'step',
            'step',
            'step',
            'next',
            'next',
            'step',
            'step',
            'continue',
        ],
            [0, 1, 2, 3, 4, 16, 20, 21, 22])

    def test_return(self):
        self._test_fib([
            'step',
            'return',
            'quit',
        ],
            [0, 1, 21],
            total_instructions=-1)

    def test_return_2(self):
        self._test_fib([
            'step',
            'step',
            'return',
            'quit',
        ],
            [0, 1, 2, 21],
            total_instructions=-1)

    def test_return_inside_call(self):
        self._test_fib([
            'step',
            'step',
            'step',
            'step',
            'step',
            'return',
            'quit',
        ],
            [0, 1, 2, 3, 4, 5, 16],
            total_instructions=-1)

    def test_break_line(self):
        self._test_fib([
            'break 6',
            'continue',
            'continue'
        ],
            [0, 22])

    def test_break_function(self):
        self._test_fib([
            'break 1',
            'step',
            'continue'  # bp never gets hit, we can't break on a function?
        ],
            [0, 1])

    def test_break_function_call(self):
        self._test_fib([
            'break 5',
            'continue',
            'continue'  # bp doesn't get hit again
        ],
            [0, 1])

    def test_break_function_return(self):
        self._test_fib([
            'break 3',
            'continue',
            'continue',  # fib(1)
            'continue',  # fib(0)
            'continue',  # fib(1)
        ],
            [0, 10, 14, 19])

    def test_rstep_one(self):
        self._test_fib(['step', 'rstep', 'continue'],
                       [0, 1, 0])

    def test_rstep_two(self):
        self._test_fib(['step', 'step', 'rstep', 'continue'],
                       [0, 1, 2, 1])

    def test_rstep_out_of_call(self):
        self._test_fib(['step', 'step', 'step', 'rstep', 'continue'],
                       [0, 1, 2, 3, 2])

    def test_rstep_arg(self):
        self._test_fib(['step', 'step', 'step', 'rstep 1', 'continue'],
                       [0, 1, 2, 3, 1])

    def test_rreturn_after_return(self):
        self._test_fib(['step'] * 2 + ['return', 'rreturn', 'continue'],
                       [0, 1, 2, 21, 1])

    def test_rreturn_depth_1(self):
        self._test_fib(['step'] * 2 + ['rreturn', 'continue'],
                       [0, 1, 2, 1])

        self._test_fib(['step'] * 3 + ['rreturn', 'continue'],
                       [0, 1, 2, 3, 1])

        self._test_fib(['step'] * 4 + ['rreturn', 'continue'],
                       [0, 1, 2, 3, 4, 1])

    def test_rreturn_depth_2(self):
        self._test_fib(['step'] * 7 + ['rreturn', 'rreturn', 'continue'],
                       [0, 1, 2, 3, 4, 5, 6, 7, 4, 1])

    def test_rnext_line(self):
        self._test_fib(['step', 'rnext', 'continue'],
                       [0, 1, 0])

    def test_rnext_call(self):
        self._test_fib(['step'] * 2 + ['rnext', 'continue'],
                       [0, 1, 2, 1])

    def test_rnext_return(self):
        self._test_fib(['step'] * 4 + ['next', 'rnext', 'continue'],
                       [0, 1, 2, 3, 4, 16, 4])

    def test_rstep_all(self):
        self._test_fib(['step' for x in range(1, 22)]
                       + ['rstep' for x in range(1, 22)]
                       + ['continue'],
                       range(22) + range(20, -1, -1))

def test_main():
    test_support.verbose = 1
    test_support.run_unittest(PdbTestCase)


if __name__ == '__main__':
    test_main()
