import sys
import cmd
import os
import types
import _odb
import linecache

debug_enabled = 0


# debug_enabled = 1
def debug(output):
    if debug_enabled:
        print >> sys.stderr, output


class Odb(cmd.Cmd):
    def __init__(self, skip=None):
        cmd.Cmd.__init__(self, completekey='tab', stdin=None, stdout=None)
        self.fncache = {}
        self.quit = 0

    # region Cmd

    def preloop(self):
        self.prompt = "(Odb)<%s>" % 0

    def postcmd(self, stop, line):
        return self.quit

    def displayhook(self, obj):
        """Custom displayhook for the exec in default(), which prevents
        assignment of the _ variable in the builtins.
        """
        # reproduce the behavior of the standard displayhook, not printing None
        if obj is not None:
            print >> self.stdout, repr(obj)

    def default(self, line):
        if line[:1] == '!': line = line[1:]
        try:
            code = compile(line + '\n', '<stdin>', 'single')
            save_stdout = sys.stdout
            save_stdin = sys.stdin
            save_displayhook = sys.displayhook
            try:
                sys.stdin = self.stdin
                sys.stdout = self.stdout
                sys.displayhook = self.displayhook
                exec code in globals(), locals()
            finally:
                sys.stdout = save_stdout
                sys.stdin = save_stdin
                sys.displayhook = save_displayhook
        except:
            t, v = sys.exc_info()[:2]
            if type(t) == type(''):
                exc_type_name = t
            else:
                exc_type_name = t.__name__
            print >> self.stdout, '***', exc_type_name + ':', v

    def do_where(self, arg):
        self.print_stack_trace()

    def do_history(self, arg):
        events = _odb.getEvents()
        for e in events:
            print e

    def do_args(self, arg):
        #arguments are the locals present during the 'call'
        print _odb.getCurrentFrame().locals

    def do_list(self, arg):
        self.lastcmd = 'list'
        curframe = _odb.getCurrentFrame()
        last = None
        if arg:
            try:
                x = eval(arg, {}, {})
                if type(x) == type(()):
                    first, last = x
                    first = int(first)
                    last = int(last)
                    if last < first:
                        # Assume it's a count
                        last = first + last
                else:
                    first = max(1, int(x) - 5)
            except:
                print >> self.stdout, '*** Error in argument:', repr(arg)
                return
        first = max(1, curframe.lineno - 5)
        if last is None:
            last = first + 10
        filename = curframe.filename
        try:
            for lineno in range(first, last + 1):
                line = linecache.getline(filename, lineno) #,self.curframe.f_globals)
                if not line:
                    print >> self.stdout, '[EOF]'
                    break
                else:
                    s = repr(lineno).rjust(3)
                    if len(s) < 4: s = s + ' '
                    s = s + ' '
                    if lineno == curframe.lineno:
                        s = s + '->'
                    print >> self.stdout, s + '\t' + line,
        except KeyboardInterrupt:
            pass

    def do_next(self, arg):
        _odb.next()

    def do_prev(self, arg):
        _odb.previous()

    def do_quit(self, arg):
        self.quit = 1

    do_q = do_quit
    do_w = do_where
    do_l = do_list

    # endregion
    def format_stack_entry(self, event, lprefix=': '):
        filename, lineno, name, = event.filename, event.lineno, event.name
        s = '%s(%r)' % (filename, lineno)
        if name:
            s = s + name
        else:
            s = s + "<lambda>"
        # if '__args__' in frame.f_locals:
        #     args = frame.f_locals['__args__']
        # else:
        #     args = None
        # if args:
        #     s = s + repr.repr(args)
        # else:
        #     s = s + '()'
        s = s + '()'
        # if '__return__' in frame.f_locals:
        #     rv = frame.f_locals['__return__']
        #     s = s + '->'
        #     s = s + repr.repr(rv)
        line = linecache.getline(filename, lineno)  # , frame.globals)
        if line: s = s + lprefix + line.strip()
        return s

    def print_stack_trace(self):
        try:
            event = _odb.getCurrentFrame()
            while event:
                self.print_stack_entry(event)
                event = event.parent
        except KeyboardInterrupt:
            pass

    def print_stack_entry(self, event, prompt_prefix='\n  ->'):
        # if frame is self.curframe:
        #     print >>self.stdout, '>',
        # else:
        print >> self.stdout, ' ',
        print >> self.stdout, self.format_stack_entry(event, prompt_prefix)

    def canonic(self, filename):
        if filename == "<" + filename[1:-1] + ">":
            return filename
        canonic = self.fncache.get(filename)
        if not canonic:
            canonic = os.path.abspath(filename)
            canonic = os.path.normcase(canonic)
            self.fncache[filename] = canonic
        return canonic

    def trace_dispatch(self, frame, event, arg):
        return self.trace_dispatch

    def run(self, filename):
        # The script has to run in __main__ namespace (or imports from
        # __main__ will break).
        #
        # So we clear up the __main__ and set several special variables
        # (this gets rid of odb's globals and cleans old variables on restarts).
        import __main__
        __main__.__dict__.clear()
        __main__.__dict__.update({"__name__": "__main__",
                                  "__file__": filename,
                                  "__builtins__": __builtins__,
                                  })
        # When odb sets tracing, a number of call and line events happens
        # BEFORE debugger even reaches user's code (and the exact sequence of
        # events depends on python version). So we take special measures to
        # avoid stopping before we reach the main script (see user_line and
        # user_call for details).
        self.mainpyfile = self.canonic(filename)
        # self._user_requested_quit = 0
        cmd = 'execfile(%r)' % filename

        import __main__
        globals = __main__.__dict__
        locals = globals

        sys.settrace(self.trace_dispatch)
        if not isinstance(cmd, types.CodeType):
            cmd = cmd + '\n'
        try:
            exec cmd in globals, locals
        finally:
            sys.settrace(None)

        self.cmdloop("Now running odb")


def main():
    if not sys.argv[1:] or sys.argv[1] in ("--help", "-h"):
        print "usage: odb.py scriptfile [arg] ..."
        sys.exit(2)

    mainpyfile = sys.argv[1]  # Get script filename
    if not os.path.exists(mainpyfile):
        print 'Error:', mainpyfile, 'does not exist'
        sys.exit(1)

    del sys.argv[0]  # Hide "odb.py" from argument list

    # Replace odb's dir with script's dir in front of module search path.
    sys.path[0] = os.path.dirname(mainpyfile)

    # Saving/restoring sys.argv: it's a good idea when sys.argv was
    # modified by the script being debugged. It's a bad idea when it was
    # changed by the user from the command line. There is a "restart" command
    # which allows explicit specification of command line arguments.
    odb = Odb()
    odb.run(mainpyfile)


# When invoked as main program, invoke the debugger on a script
if __name__ == '__main__':
    import tdb.odb

    tdb.odb.main()
