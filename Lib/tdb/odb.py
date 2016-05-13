import sys
import cmd
import os
import types
import _odb
import linecache

NAVIGATION_COMMAND_FLAG = 1

debug_enabled = 0


# debug_enabled = 1
def debug(output):
    if debug_enabled:
        print >> sys.stderr, output


class Odb(cmd.Cmd):
    def __init__(self, stdin=None, stdout=None, skip=None):
        cmd.Cmd.__init__(self, completekey='tab', stdin=stdin, stdout=stdout)
        self.fncache = {}
        self.quit = 0
        self.lineno = None

    def reset(self):
        self.lineno = None

    def get_current_frame(self):
        return _odb.getCurrentFrame()

    def get_current_event(self):
        return _odb.getCurrentEvent()

    def get_current_timestamp(self):
        return _odb.getCurrentTimestamp()

    def get_current_frame_id(self):
        return _odb.getCurrentFrameId()

    def get_current_lineno(self):
        return self.get_current_event().lineno

    # region Cmd

    def preloop(self):
        frame = _odb.getCurrentFrame()
        if frame:
            event = _odb.getCurrentEvent()
            if event.is_return():
                print >> self.stdout, "--Return--"

            if event.is_call():
                print >> self.stdout, "--Call--"

            self.print_stack_entry(frame.filename, event.lineno, frame.name, frame.return_value)

        self.prompt = "(Odb)<%s>" % _odb.getCurrentTimestamp();

    def postcmd(self, stop, line):
        return stop or self.quit

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
                exec code in _odb.getGlobals(), _odb.getCurrentLocals()
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
        '''
        Prints a stacktrace for the current position
        '''
        self.print_stack_trace()

    def do_history(self, arg):
        '''
        Lists the events recorded by Odb
        '''
        events = _odb.getEvents()
        for e in events:
            print e

    def do_chistory(self, arg):
        '''
        Lists the calls / stack frames recorded by Odb
        '''
        frames = _odb.getFrames()
        for f in frames:
            self.print_stack_entry(f.filename, f.lineno, f.name)

    def do_vhistory(self, arg):
        '''
        Displays a list of values which the variable had in the past
        <0> 5 means at timestamp <0> the value was 5
        '''
        values = _odb.getLocalHistory(arg)
        if values:
            for v in values:
                print v

    def do_args(self, arg):
        '''
        Lists the arguments of the current function
        '''
        # arguments are the locals present during the 'call'
        print _odb.getFrameArguments()

    def do_retval(self, arg):
        frame = _odb.getCurrentFrame()
        print >> self.stdout, "%s returns %s at %s" % (frame.name, frame.return_value, frame.return_timestamp)

    def do_locals(self, arg):
        '''
        Lists the local variables of the current frame
        '''
        print _odb.getCurrentLocals()

    def do_globals(self, arg):
        '''
        Lists the global variables
        '''
        print _odb.getGlobals()

    def do_list(self, arg):
        '''
        Lists the 10 lines of code around the current instruction
        Subsequent 'list' insturctions will show more lines
        '''
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
        elif self.lineno is None:
            first = max(1, self.get_current_lineno() - 5)
        else:
            first = self.lineno + 1
        if last is None:
            last = first + 10
        filename = curframe.filename
        try:
            for lineno in range(first, last + 1):
                line = linecache.getline(filename, lineno)  # ,self.curframe.f_globals)
                if not line:
                    print >> self.stdout, '[EOF]'
                    break
                else:
                    s = repr(lineno).rjust(3)
                    if len(s) < 4: s = s + ' '
                    s = s + ' '
                    if lineno == self.get_current_lineno():
                        s = s + '->'
                    print >> self.stdout, s + '\t' + line,
                    self.lineno = lineno
        except KeyboardInterrupt:
            pass

    def do_up(self, arg):
        '''
        Moves up one frame in the call stack
        To see the call stack at the current timestamp use 'where'
        '''
        _odb.moveUpFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_down(self, arg):
        '''
        Moves down one frame in the call stack
        To see the call stack at the current timestamp use 'where'
        '''
        _odb.moveDownFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_nextf(self, arg):
        '''
        Moves to the first timestamp in the next frame in terms of the chronological order in which frames were
        placed on the call stack
        To see a list of frames use 'chistory'
        To see the current call stack use 'where'
        '''
        _odb.moveNextFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_prevf(self, arg):
        '''
        Moves to the first timestamp in the previous frame in terms of the chronological order in which frames were
        placed on the call stack
        To see a list of frames use 'chistory'
        To see the current call stack use 'where'
        '''
        _odb.movePrevFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_step(self, arg):
        '''
        Steps forward one instruction / timestep
        '''
        _odb.do_step()
        return NAVIGATION_COMMAND_FLAG

    def do_rstep(self, arg):
        '''
        Steps backwards one instruction / timestep
        '''
        _odb.do_rstep()
        return NAVIGATION_COMMAND_FLAG

    def do_next(self, arg):
        '''
        Steps forward to the next instruction in the current function OR the return of the current function
        Steps over any function calls
        '''
        _odb.do_next()
        return NAVIGATION_COMMAND_FLAG

    def do_rnext(self, arg):
        '''
        Steps backwards to the next instruction in the current function OR the call of the current function
        Steps over any function calls
        '''
        _odb.do_rnext()
        return NAVIGATION_COMMAND_FLAG

    def do_return(self, arg):
        '''
        Steps to the return of the call of the current frame / function
        '''
        _odb.do_return()
        return NAVIGATION_COMMAND_FLAG

    def do_rreturn(self, arg):
        '''
        Steps to the call of the current frame / function
        '''
        _odb.do_rreturn()
        return NAVIGATION_COMMAND_FLAG

    def do_jump(self, arg):
        '''
        Jumps to the specified timestamp
        '''
        try:
            arg = int(arg)
        except ValueError:
            print >> self.stdout, "*** The 'jump' command requires a line number."
        else:
            _odb.do_jump(arg)
        return NAVIGATION_COMMAND_FLAG

    def do_quit(self, arg):
        self.quit = 1

    do_q = do_quit
    do_w = do_where
    do_l = do_list
    do_rv = do_retval

    do_u = do_up
    do_d = do_down
    do_nf = do_nextf
    do_pf = do_prevf
    do_s = do_step
    do_rs = do_rstep
    do_n = do_next
    do_rn = do_rnext

    # endregion

    def format_stack_entry(self, filename, lineno, name, returnvalue=None, lprefix=': '):
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
        if returnvalue:
            s = s + '->'
            s = s + repr(returnvalue)
        line = linecache.getline(filename, lineno)  # , frame.globals)
        if line: s = s + lprefix + line.strip()
        return s

    def _getval(self, arg):
        try:
            return eval(arg, _odb.getCurrentLocals())
        except:
            t, v = sys.exc_info()[:2]
            if isinstance(t, str):
                exc_type_name = t
            else:
                exc_type_name = t.__name__
            print >> self.stdout, '***', exc_type_name + ':', repr(v)
            raise

    def do_p(self, arg):
        '''
        Prints the value of the expression evaluated in the current locals
        '''
        try:
            print >> self.stdout, repr(self._getval(arg))
        except:
            pass

    def print_stack_trace(self):
        try:
            currentframe = _odb.getCurrentFrame()
            event = _odb.getCurrentEvent()
            frames = []
            frame = currentframe.parent if currentframe else None

            while frame:
                frames.append(frame)
                frame = frame.parent

            for f in reversed(frames):
                # For the other frames the call site is correct
                self.print_stack_entry(f.filename, f.lineno, f.name)

            # Use the most accureate line number for the actual line
            if currentframe:
                self.print_stack_entry(currentframe.filename, event.lineno, currentframe.name)

        except KeyboardInterrupt:
            pass

    def print_stack_entry(self, filename, lineno, name, returnvalue=None, prompt_prefix='\n  ->'):
        # if frame is self.curframe:
        #     print >>self.stdout, '>',
        # else:
        print >> self.stdout, ' ',
        print >> self.stdout, self.format_stack_entry(filename, lineno, name, returnvalue, prompt_prefix)

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

    def control_loop(self):
        while not self.quit:
            self.cmdloop()
            self.reset()

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

        globals = _odb.initializeGlobals( __main__.__dict__)
        locals = globals

        _odb.reset()
        sys.settrace(self.trace_dispatch)
        if not isinstance(cmd, types.CodeType):
            cmd = cmd + '\n'
        try:
            exec cmd in globals, locals
        finally:
            sys.settrace(None)

        print "Program has finished, now entering ODB mode"
        _odb.setup()
        self.control_loop()


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
