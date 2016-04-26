'''
A model for testing execution of the program fib.py
This model contains the line number and call depth for every step in the execution of the program
Additionally, at each step the instruction count which would be reached if a next, rnext, return or rreturn
function is called is also stored

The model keeps an instruction counter to track which instruction ins currently being executed
A test class need only call the do_*** method corresponding to each action passed into a debugger
The model then checks that the debugger has reached the correct instruction

The model's methods are intended to be as simple as possible so that there is no posibility they are wrong
Instead, any complexity is captured by the model itself
Each method is essentially 'self.ic = self.model[self.ic].SOME_PROPERTY'

To use run 'fibexecutionmodel.generate_tests(XXXTestCase)' where XXXTestCase is a class
which extends unittest.TestCase
This will generate tests for all the execution points in the model for following instructions:
    step, rstep, next, rnext, return, rreturn

TODO: currently has code specific to Odb. This should be changed to a general interface
'''
class FibExecutionModel():
    class ExecutionPoint():
        def __init__(self, lineno, call_depth, next_ic, rnext_ic, return_ic, rreturn_ic):
            self.lineno = lineno
            self.call_depth = call_depth
            self.next_ic = next_ic
            self.rnext_ic = rnext_ic
            self.return_ic = return_ic
            self.rreturn_ic = rreturn_ic

    model = [
        ExecutionPoint(1, 0, 1, 0, 23, 0),
        ExecutionPoint(5, 0, 22, 0, 23, 0),
        ExecutionPoint(1, 1, 3, 1, 21, 1),  # fib(3) returns at 21
        ExecutionPoint(2, 1, 4, 2, 21, 2),
        ExecutionPoint(4, 1, 21, 3, 21, 2),
        # 5,
        ExecutionPoint(1, 2, 6, 4, 16, 4),  # fib(2) returns at 16
        ExecutionPoint(2, 2, 7, 5, 16, 5),
        ExecutionPoint(4, 2, 16, 6, 16, 5),
        ExecutionPoint(1, 3, 9, 7, 11, 7),  # fib(1) returns at 11
        ExecutionPoint(2, 3, 10, 8, 11, 8),
        # 10,
        ExecutionPoint(3, 3, 11, 9, 11, 8),
        ExecutionPoint(3, 2, 16, 10, 12, 8),
        ExecutionPoint(1, 3, 13, 7, 15, 11),  # fib(0) returns at 15
        ExecutionPoint(2, 3, 14, 12, 15, 12),
        ExecutionPoint(3, 3, 15, 13, 15, 12),
        # 15,
        ExecutionPoint(3, 2, 16, 14, 16, 12),
        ExecutionPoint(4, 1, 21, 7, 17, 5),
        ExecutionPoint(1, 2, 18, 4, 20, 16),  # fib(1) returns at 20
        ExecutionPoint(2, 2, 19, 17, 20, 17),
        ExecutionPoint(3, 2, 20, 18, 20, 17),
        # 20,
        ExecutionPoint(3, 1, 21, 19, 21, 17),
        ExecutionPoint(4, 0, 22, 4, 22, 2),
        ExecutionPoint(6, 0, 23, 1, 23, 0),
        ExecutionPoint(6, 0, 23, 22, 23, 0),
    ]

    filename = 'examples/fib.py'

    def __init__(self, debugger, testcase):
        self.ic = 0
        self.debugger = debugger
        self.testcase = testcase

    def jump(self, i):
        self.ic = i
        self.check_model()

    def do_step(self):
        if self.ic < len(self.model) - 1:
            self.ic = self.ic + 1
        self.check_model()

    def do_rstep(self):
        if self.ic > 0:
            self.ic = self.ic - 1
        self.check_model()

    def do_next(self):
        self.ic = self.model[self.ic].next_ic
        self.check_model()

    def do_rnext(self):
        self.ic = self.model[self.ic].rnext_ic
        self.check_model()

    def do_return(self):
        self.ic = self.model[self.ic].return_ic
        self.check_model()

    def do_rreturn(self):
        self.ic = self.model[self.ic].rreturn_ic
        self.check_model()

    def check_model(self):
        self.check_timestamp()

    def check_timestamp(self):
        expected = self.ic
        actual = self.debugger.get_current_timestamp()
        self.testcase.assertEqual(expected, actual, 'Expected ic %s actual %s' % (expected, actual))

    def check_lineno(self):
        expected = self.model[self.ic].lineno
        actual = self.debugger.get_current_lineno()
        self.testcase.assertEqual(expected, actual, 'Expected lineno %s actual %s' % (expected, actual))


def create_test(i, command, function):
    def _test_(self):
        def control_function(model):
            yield 'jump ' + str(i);
            model.jump(i)
            yield command
            function(model)
            yield 'quit'

        self._test_fib(control_function)

    return _test_


def generate_test(testcaseclass, method, test_name):
    test_method = method
    test_method.__name__ = test_name
    setattr(testcaseclass, test_method.__name__, test_method)


def generate_tests(testcaseclass):
    for i in range(len(FibExecutionModel.model)):
        generate_test(testcaseclass, create_test(i, 'step', FibExecutionModel.do_step), 'test_step_%s' % i)
        generate_test(testcaseclass, create_test(i, 'rstep', FibExecutionModel.do_rstep), 'test_rstep_%s' % i)
        generate_test(testcaseclass, create_test(i, 'next', FibExecutionModel.do_next), 'test_next_%s' % i)
        generate_test(testcaseclass, create_test(i, 'rnext', FibExecutionModel.do_rnext), 'test_rnext_%s' % i)
        generate_test(testcaseclass, create_test(i, 'return', FibExecutionModel.do_return), 'test_return_%s' % i)
        generate_test(testcaseclass, create_test(i, 'rreturn', FibExecutionModel.do_rreturn), 'test_rreturn_%s' % i)
        pass
