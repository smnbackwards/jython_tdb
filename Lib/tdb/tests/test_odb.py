import sys
import unittest
import fibexecutionmodel
from tdb import odb
from test import test_support

class NullDevice():
    def write(self, s):
        pass


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
        # sys.stdout = NullDevice()
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

    #region model based tests
    class OdbExecutionModel():
        def __init__(self, frame_id, up_frame_timestamp, down_frame_timestamp, next_frame_timestamp, prev_frame_timestamp):
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

    #endregion

#region Test Generation
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
    generate_test(i, create_test_jump, 'test_jump_%s'%i)
    generate_test(i, create_test_up, 'test_up_%s' % i)
    generate_test(i, create_test_down, 'test_down_%s' % i)
    generate_test(i, create_test_next, 'test_next_%s' % i)
    generate_test(i, create_test_prev, 'test_prev_%s' % i)
#endregion

class OdbFibModelTestCase(unittest.TestCase):
    def _test_fib(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb()
            model = fibexecutionmodel.FibExecutionModel(debugger, self)
            test_input.initialize_generator(commands_and_asserts_generator(model))
            debugger.run(model.filename)


fibexecutionmodel.generate_tests(OdbFibModelTestCase)

def test_main():
    # test_support.verbose = 1
    test_support.run_unittest(OdbFibFrameModelTestCase, OdbFibModelTestCase)


if __name__ == '__main__':
    test_main()
