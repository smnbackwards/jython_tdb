import sys
import unittest
import fibexecutionmodel
import os
import _odb
from tdb import odb
from test import test_support


class _GeneratorInput:
    def initialize_generator(self, generator):
        self.generator = generator

    def readline(self):
        line = next(self.generator)
        print line
        return line + '\n'


class TestGeneratorInput(object):
    def initialize_generator(self, generator):
        self.generator_input.initialize_generator(generator)

    def __enter__(self):
        self.real_stdin = sys.stdin
        self.real_stdout = sys.stdout
        self.generator_input = _GeneratorInput()
        sys.stdin = self.generator_input
        sys.stdout = open(os.devnull, 'w')
        return self

    def __exit__(self, *exc):
        sys.stdin = self.real_stdin
        sys.stdout = self.real_stdout


class OdbFibFrameModelTestCase(unittest.TestCase):
    fib = 'examples/fib.py'

    def _test_fib(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb(stdin=sys.stdin, stdout=sys.stdout)
            test_input.initialize_generator(commands_and_asserts_generator(debugger))
            debugger.run(self.fib)

    def _test(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb()
            test_input.initialize_generator(commands_and_asserts_generator(debugger))
            debugger.run(self.fib)

    def test_continue(self):
        def commands_and_asserts_generator(debugger):
            yield 'break 4'
            yield 'continue' ; self.assertEqual(4, debugger.get_current_timestamp())
            yield 'continue' ; self.assertEqual(7, debugger.get_current_timestamp())
            yield 'continue' ; self.assertEqual(16, debugger.get_current_timestamp())
            yield 'continue' ; self.assertEqual(21, debugger.get_current_timestamp())
            yield 'continue' ; self.assertEqual(23, debugger.get_current_timestamp())
            yield 'continue' ; self.assertEqual(23, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_rcontinue(self):
        def commands_and_asserts_generator(debugger):
            yield 'continue' ; self.assertEqual(23, debugger.get_current_timestamp())
            yield 'break 4'
            yield 'rcontinue' ; self.assertEqual(21, debugger.get_current_timestamp())
            yield 'rcontinue' ; self.assertEqual(16, debugger.get_current_timestamp())
            yield 'rcontinue' ; self.assertEqual(7, debugger.get_current_timestamp())
            yield 'rcontinue' ; self.assertEqual(4, debugger.get_current_timestamp())
            yield 'rcontinue' ; self.assertEqual(0, debugger.get_current_timestamp())
            yield 'rcontinue' ; self.assertEqual(0, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_break_next(self):
        def commands_and_asserts_generator(debugger):
            yield 'break 2'
            yield 'next' ; self.assertEqual(1, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(3, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(4, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(6, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(7, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(9, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(10, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(11, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(13, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(14, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(15, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(16, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(18, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(19, debugger.get_current_timestamp())
            yield 'next' ; self.assertEqual(20, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_break_rnext(self):
        def commands_and_asserts_generator(debugger):
            yield 'jump 23'
            yield 'break 2'
            yield 'rnext' ; self.assertEqual(22, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(18, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(17, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(13, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(12, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(9, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(8, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(7, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(6, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(5, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(4, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(3, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(2, debugger.get_current_timestamp())
            yield 'rnext' ; self.assertEqual(1, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_break_return(self):
        def commands_and_asserts_generator(debugger):
            yield 'break 2'
            yield 'return' ; self.assertEqual(3, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_break_rnext(self):
        def commands_and_asserts_generator(debugger):
            yield 'jump 23'
            yield 'break 2'
            yield 'rreturn' ; self.assertEqual(18, debugger.get_current_timestamp())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    # region model based tests
    class OdbExecutionModel():
        def __init__(self, frame_id, up_frame_timestamp, down_frame_timestamp, next_frame_timestamp,
                     prev_frame_timestamp):
            self.frame_id = frame_id
            self.up_frame_timestamp = up_frame_timestamp
            self.down_frame_timestamp = down_frame_timestamp
            self.next_frame_timestamp = next_frame_timestamp
            self.prev_frame_timestamp = prev_frame_timestamp

    model = [
        OdbExecutionModel(0, 0, 2,  2,  0),
        OdbExecutionModel(0, 1, 2,  2,  1), #no prev or up frame, so no change
        OdbExecutionModel(1, 0, 5,  5,  0),
        OdbExecutionModel(1, 0, 5,  5,  0),
        OdbExecutionModel(1, 0, 5,  5,  0),
        OdbExecutionModel(2, 2, 8,  8,  2),
        OdbExecutionModel(2, 2, 8,  8,  2),
        OdbExecutionModel(2, 2, 8,  8,  2),
        OdbExecutionModel(3, 5, 8,  12, 5),
        OdbExecutionModel(3, 5, 9,  12, 5),
        OdbExecutionModel(3, 5, 10, 12, 5),
        OdbExecutionModel(3, 5, 11, 12, 5),  # 11
        OdbExecutionModel(4, 5, 12, 17, 8),
        OdbExecutionModel(4, 5, 13, 17, 8),
        OdbExecutionModel(4, 5, 14, 17, 8),
        OdbExecutionModel(4, 5, 15, 17, 8),  # 15
        OdbExecutionModel(2, 2, 16, 8,  2),  # 16
        OdbExecutionModel(5, 2, 17, 17, 12),
        OdbExecutionModel(5, 2, 18, 18, 12),
        OdbExecutionModel(5, 2, 19, 19, 12),
        OdbExecutionModel(5, 2, 20, 20, 12),  # 20
        OdbExecutionModel(1, 0, 21, 5,  0),  # 21
        OdbExecutionModel(0, 22,22, 2,  22),
        OdbExecutionModel(0, 23,23, 2,  23),
    ]

    def setUp(self):
        super(OdbFibFrameModelTestCase, self).setUp()
        self.timestamp = len(self.model) - 1

    def jump(self, n):
        self.timestamp = n

    def up(self):
        self.timestamp = self.model[self.timestamp].up_frame_timestamp

    def down(self):
        self.timestamp = self.model[self.timestamp].down_frame_timestamp

    def next_frame(self):
        self.timestamp = self.model[self.timestamp].next_frame_timestamp

    def prev_frame(self):
        self.timestamp = self.model[self.timestamp].prev_frame_timestamp

    def compare_with_model(self, debugger):
        expected_timestamp = self.timestamp
        actual_timestamp = debugger.get_current_timestamp()

        actual_frame_id = self.model[self.timestamp].frame_id
        expected_frame_id = debugger.get_current_frame_id()

        self.assertEqual(expected_timestamp, actual_timestamp)
        self.assertEqual(actual_frame_id, expected_frame_id)

        # endregion


# region Test Generation
def create_test_jump(i):
    def _test_jump(self):
        def control_function(debugger):
            yield 'jump ' + str(i);
            self.jump(i)
            self.compare_with_model(debugger)
            yield 'quit'

        self._test_fib(control_function)

    return _test_jump


def create_test_up(i):
    def _test_up(self):
        def control_function(debugger):
            yield 'jump ' + str(i);
            self.jump(i)
            self.compare_with_model(debugger)
            yield 'up';
            self.up()
            self.compare_with_model(debugger)
            yield 'quit'

        self._test_fib(control_function)

    return _test_up


def create_test_down(i):
    def _test_down(self):
        def control_function(debugger):
            yield 'jump ' + str(i);
            self.jump(i)
            self.compare_with_model(debugger)
            yield 'down';
            self.down()
            self.compare_with_model(debugger)
            yield 'quit'

        self._test_fib(control_function)

    return _test_down

def create_test_next(i):
    def _test_next(self):
        def control_function(debugger):
            yield 'jump ' + str(i);
            self.jump(i)
            self.compare_with_model(debugger)
            yield 'nextf';
            self.next_frame()
            self.compare_with_model(debugger)
            yield 'quit'

        self._test_fib(control_function)

    return _test_next


def create_test_prev(i):
    def _test_prev(self):
        def control_function(debugger):
            yield 'jump ' + str(i);
            self.jump(i)
            self.compare_with_model(debugger)
            yield 'prevf';
            self.prev_frame()
            self.compare_with_model(debugger)
            yield 'quit'

        self._test_fib(control_function)

    return _test_prev


def generate_test(i, method, test_name):
    test_method = method(i)
    test_method.__name__ = test_name
    setattr(OdbFibFrameModelTestCase, test_method.__name__, test_method)


for i in range(len(OdbFibFrameModelTestCase.model)):
    generate_test(i, create_test_jump, 'test_jump_%s' % i)
    generate_test(i, create_test_up, 'test_up_%s' % i)
    generate_test(i, create_test_down, 'test_down_%s' % i)
    generate_test(i, create_test_next, 'test_next_%s' % i)
    generate_test(i, create_test_prev, 'test_prev_%s' % i)


# endregion

class OdbFibModelTestCase(unittest.TestCase):
    def _test_fib(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb()
            model = fibexecutionmodel.FibExecutionModel(debugger, self)
            test_input.initialize_generator(commands_and_asserts_generator(model))
            debugger.run(model.filename)
            # ensure there is no input remaining
            with self.assertRaises(StopIteration):
                next(test_input.generator_input.generator)


fibexecutionmodel.generate_tests(OdbFibModelTestCase)

class OdbExceptionModelTestCase(unittest.TestCase):
    filename = 'examples/exception.py'

    def _test(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb()
            test_input.initialize_generator(commands_and_asserts_generator(debugger))
            debugger.run(self.filename)

    def test_linenumbers(self):
        def commands_and_asserts_generator(debugger):
            linenumbers = [2, 8, 12, 2, 3, 4, 4, 5, 6, 6,
                           13, 14, 8, 9, 9, 9, 14, 15, 17, 18,
                           19, 20, 24, 26, 27, 31, 33, 35, 35, 35]
            self.assertEqual(linenumbers, _odb.getLinenos())
            yield 'quit'
        self._test(commands_and_asserts_generator)

    def test_events(self):
        def commands_and_asserts_generator(debugger):
            types = ['L', 'L', 'L', 'C', 'L', 'L', 'E', 'L', 'L', 'R',
                     'L', 'L', 'C', 'L', 'E', 'R', 'E', 'L', 'L', 'L',
                     'L', 'L', 'L', 'L', 'L', 'L', 'L', 'L', 'E', 'R']
            self.assertEqual(types, [e[0] for e in _odb.getEventTypes()])
            yield 'quit'
        self._test(commands_and_asserts_generator)

def test_main():
    # test_support.verbose = 1
    test_support.run_unittest(
        OdbFibFrameModelTestCase,
        OdbFibModelTestCase,
        OdbExceptionModelTestCase
    )


if __name__ == '__main__':
    test_main()
