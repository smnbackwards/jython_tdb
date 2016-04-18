import sys
import unittest
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
        # self.real_stdout = sys.stdout
        self.generator_input = _GeneratorInput()
        sys.stdin = self.generator_input
        # sys.stdout = NullDevice()
        return self

    def __exit__(self, *exc):
        sys.stdin = self.real_stdin
        # sys.stdout = self.real_stdout

class OdbTestCase(unittest.TestCase):
    fib = 'examples/fib.py'
    def _test_fib(self, commands_and_asserts_generator):
        with TestGeneratorInput() as test_input:
            debugger = odb.Odb()
            test_input.initialize_generator(commands_and_asserts_generator(debugger))
            debugger.run(self.fib)

    def test_jump_generator(self):

        def control_function(debugger):
            self.assertEqual(22, debugger.get_current_timestamp())
            yield 'jump 0'
            self.assertEqual(0, debugger.get_current_timestamp())
            yield 'quit'

        self._test_fib(control_function)


    def test_callstack_traversal(self):
        def control_function(debugger):
            yield 'jump 17'
            self.assertEqual(17, debugger.get_current_timestamp())
            self.assertEqual(4, debugger.get_current_frame_id())
            yield 'up'
            self.assertEqual(2, debugger.get_current_timestamp())
            self.assertEqual(0, debugger.get_current_frame_id())
            yield 'quit'

        self._test_fib(control_function)







def test_main():
    # test_support.verbose = 1
    test_support.run_unittest(OdbTestCase)


if __name__ == '__main__':
    test_main()
