import unittest

from tdb.basedebugger import BaseDebugger

def test2(n):
    for i in range(n):
        print str(n)

def test(n):
    print "hello"
    print "hello"
    print "hello"
    print "hello"
    print "hello"
    print "hello"
    print "hello"
    test2(n)

class myDebugger(BaseDebugger):

    run = 0

    def user_call(self, frame, args):
        name = frame.f_code.co_name or "<unknown>"
        print "call", name, args
        # self.set_continue() # continue

    def user_line(self, frame):
        if self.run:
            self.run = 0
            self.set_trace() # start tracing
        else:
            # arrived at breakpoint
            name = frame.f_code.co_name or "<unknown>"
            filename = self.canonic(frame.f_code.co_filename)
            print "break at", filename, frame.f_lineno, "in", name
        print "continue..."
        # self.set_continue() # continue to next breakpoint

    def user_return(self, frame, value):
        name = frame.f_code.co_name or "<unknown>"
        print "return from", name, value
        print "continue..."
        self.set_continue() # continue

    def user_exception(self, frame, exception):
        name = frame.f_code.co_name or "<unknown>"
        print "exception in", name, exception
        print "continue..."
        self.set_continue() # continue




class BaseDebuggerTestCase(unittest.TestCase):
    def runTest(self):
        db = myDebugger()
        db.run = 1
        db.set_break("test_basedebugger.py", 11)
        db.set_break("test_basedebugger.py", 7)
        db.runcall(test, 1)

if __name__ == '__main__':
    unittest.main()