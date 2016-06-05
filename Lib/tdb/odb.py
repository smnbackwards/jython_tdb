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
    default_stdout_size = 10

    def __init__(self, stdin=None, stdout=None, skip=None):
        cmd.Cmd.__init__(self, completekey='tab', stdin=stdin, stdout=stdout)
        self.fncache = {}
        self.quit = 0
        self.lineno = None
        self.prev_timestamp = 0
        self.stdoutno = self.default_stdout_size
        self.event_timestamp = 0
        self.frame_id = None


    def reset(self):
        self.lineno = None
        self.stdoutno = self.default_stdout_size
        self.event_timestamp = None
        self.frame_id = None

    def get_current_frame(self):
        return _odb.getCurrentFrame()

    def get_current_timestamp(self):
        return _odb.getCurrentTimestamp()

    def get_current_frame_id(self):
        return _odb.getCurrentFrameId()

    def get_current_lineno(self):
        return _odb.getCurrentEventLineno()

    # region Cmd

    def preloop(self):
        output = self.recorded_stdout.getBetween(self.prev_timestamp, _odb.getCurrentTimestamp())
        if output:
            print >> self.stdout, output

        frame = _odb.getCurrentFrame()
        if frame:
            event_type = _odb.getCurrentEventType()
            event_lineno = _odb.getCurrentEventLineno()
            if event_type == 'RETURN':
                print >> self.stdout, "--Return--"

            if event_type == 'CALL':
                print >> self.stdout, "--Call--"

            if event_type == 'EXCEPTION':
                exception = _odb.getCurrentException()
                if type(exception.type) == type(''):
                    exc_type_name = exception.type
                else: exc_type_name = exception.type.__name__
                print >>self.stdout, exc_type_name + ':', repr(exception.value)
                # TODO traceback?

            self.print_stack_entry(frame.filename, event_lineno, frame.name, frame.return_value)

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

    def do_events(self, arg):
        '''
        Lists the events recorded by Odb
        events n will display the 20 events around timestmap n
        Successive calls will display 20 more events
        '''
        current_timestamp = self.get_current_timestamp()
        if arg:
            #removed the argument, so that repeated calls work correctly
            self.lastcmd = 'events'
            try:
                first = int(arg)
            except:
                print >> self.stdout, '*** Error in argument:', repr(arg)
                return
        elif self.event_timestamp is None:
            first = max(1, current_timestamp - 5)
        else:
            first = self.event_timestamp + 1

        last = first + 20
        try:
            for timestamp in range(first, last + 1):
                event = _odb.getEvent(timestamp)
                if not event:
                    print >> self.stdout, '[EOF]'
                    break
                else:
                    s = ''
                    if timestamp == current_timestamp:
                        s = '->'
                    print >> self.stdout, s + '\t' + event,
                    self.event_timestamp = timestamp
        except KeyboardInterrupt:
            pass

    def do_frames(self, arg):
        '''
        Lists the calls / stack frames recorded by Odb
        '''
        current_frame_id = self.get_current_frame_id()
        if arg:
            #removed the argument, so that repeated calls work correctly
            self.lastcmd = 'frames'
            try:
                first = int(arg)
            except:
                print >> self.stdout, '*** Error in argument:', repr(arg)
                return
        elif self.frame_id is None:
            first = max(1, current_frame_id - 5)
        else:
            first = self.frame_id + 1

        last = first + 20
        try:
            for f in _odb.getFrames(first,last):
                s = repr(f.index).rjust(3)
                if len(s) < 4: s = s + ' '
                s = s + ' '
                if f.index == current_frame_id:
                    s = s + '->'
                print >> self.stdout, '%s \t <%d>%s:%d'%(s, f.timestamp, f.name, f.lineno)
                self.frame_id = f.index
        except KeyboardInterrupt:
            pass

    def do_history(self, arg):
        '''
        Displays a list of values which the variable had in the past
        <0> 5 means at timestamp <0> the value was 5
        '''
        if not arg:
            print >> self.stdout, 'history requires a variable name'
        values = _odb.getLocalHistory(arg)
        if values:
            try:
                for v in values:
                    timestamp = '<%s>'%v.getTimestamp();
                    print >> self.stdout, '%s : %s'%(timestamp.ljust(10), v.getValue())
            except KeyboardInterrupt:
                pass

    def do_eval(self, arg):
        try:
            save_stdout = sys.stdout
            save_stdin = sys.stdin
            save_displayhook = sys.displayhook
            try:
                sys.stdin = self.stdin
                sys.stdout = self.stdout
                sys.displayhook = self.displayhook
                frame = _odb.getCurrentFrame()

                results = _odb.eval(arg)
                for r in results:
                    print r
            finally:
                sys.stdout = save_stdout
                sys.stdin = save_stdin
                sys.displayhook = save_displayhook
        except KeyboardInterrupt:
            pass

    def do_args(self, arg):
        '''
        Lists the arguments of the current function
        '''
        # arguments are the locals present during the 'call'
        print _odb.getFrameArguments()

    def do_retval(self, arg):
        '''
        Lists the return value of the current function / frame
        '''
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

    def do_out(self, args):
        '''
        displays the last 10 lines of stdout
        'out n' displays 10 lines of stdout before instuction count n
        '''
        if self.stdoutno == -1:
            print >> self.stdout, '*** End of stdout'
            return

        output = self.recorded_stdout.getLastN(self.get_current_timestamp(), self.stdoutno)
        if len(output) == 0 :
            print >> self.stdout, '*** No stdout to display'
        else :
            print >> self.stdout, 'Showing previous', len(output), 'stdout values'
            print >> self.stdout, '\t'+'\t'.join(output)
            if self.stdoutno > len(output):
                self.stdoutno = -1
            else :
                self.stdoutno += self.default_stdout_size

    def do_up(self, arg):
        '''
        Moves up one frame in the call stack
        To see the call stack at the current timestamp use 'where'
        '''
        self.prev_timestamp = _odb.moveUpFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_down(self, arg):
        '''
        Moves down one frame in the call stack
        To see the call stack at the current timestamp use 'where'
        '''
        self.prev_timestamp = _odb.moveDownFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_nextf(self, arg):
        '''
        Moves to the first timestamp in the next frame in terms of the chronological order in which frames were
        placed on the call stack
        To see a list of frames use 'frames'
        To see the current call stack use 'where'
        '''
        self.prev_timestamp = _odb.moveNextFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_prevf(self, arg):
        '''
        Moves to the first timestamp in the previous frame in terms of the chronological order in which frames were
        placed on the call stack
        To see a list of frames use 'frames'
        To see the current call stack use 'where'
        '''
        self.prev_timestamp = _odb.movePrevFrames()
        return NAVIGATION_COMMAND_FLAG

    def do_step(self, arg):
        '''
        Steps forward one instruction / timestep
        '''
        self.prev_timestamp = _odb.do_step()
        return NAVIGATION_COMMAND_FLAG

    def do_rstep(self, arg):
        '''
        Steps backwards one instruction / timestep
        '''
        self.prev_timestamp = _odb.do_rstep()
        return NAVIGATION_COMMAND_FLAG

    def do_next(self, arg):
        '''
        Steps forward to the next instruction in the current function OR the return of the current function
        Steps over any function calls
        '''
        self.prev_timestamp = self.get_current_timestamp() + 1
        _odb.do_next()
        return NAVIGATION_COMMAND_FLAG

    def do_rnext(self, arg):
        '''
        Steps backwards to the next instruction in the current function OR the call of the current function
        Steps over any function calls
        '''
        self.prev_timestamp = _odb.do_rnext()
        return NAVIGATION_COMMAND_FLAG

    def do_return(self, arg):
        '''
        Steps to the return of the call of the current frame / function
        '''
        self.prev_timestamp = self.get_current_timestamp()
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
            self.prev_timestamp = _odb.do_jump(arg)
        return NAVIGATION_COMMAND_FLAG

    def do_continue(self, arg):
        '''
        Continues execution forwards until a breakpoint is reached or the program exits
        '''
        self.prev_timestamp = self.get_current_timestamp()
        _odb.do_continue()
        return NAVIGATION_COMMAND_FLAG

    def do_rcontinue(self, arg):
        '''
        Continues execution backwards until a breakpoint is reached or the program's invocation is reached
        '''
        self.prev_timestamp = _odb.do_rcontinue()
        return NAVIGATION_COMMAND_FLAG


    def do_break(self, arg, temporary = 0):
        '''
        break [[filename:]lineno]
        sets a breakpoint
        '''
        if not arg:
            # no argument, so we print the breakpoints
            for b in _odb.getBreakpoints():
                print b
            return

        # parse arguments; comma has lowest precedence
        # and cannot occur in filename
        filename = None
        lineno = None

        colon = arg.rfind(':')
        if colon >= 0:
            filename = arg[:colon].rstrip()
            f = self.lookupmodule(filename)
            if not f:
                print >>self.stdout, '*** ', repr(filename),
                print >>self.stdout, 'not found from sys.path'
                return
            else:
                filename = f
            arg = arg[colon+1:].lstrip()
            try:
                lineno = int(arg)
            except ValueError, msg:
                print >>self.stdout, '*** Bad lineno:', arg
                return
        else:
            # no colon; can be lineno
            try:
                lineno = int(arg)
            except ValueError:

                return
        if not filename:
            filename = self.defaultFile()
        # Check for reasonable breakpoint
        line = self.checkline(filename, lineno)
        if line:
            # now set the break point
            print _odb.setBreakpoint(filename, line)

    do_b = do_break

    def defaultFile(self):
        """Produce a reasonable default."""
        filename = _odb.getCurrentFrame().filename
        if filename == '<string>' and self.mainpyfile:
            filename = self.mainpyfile
        return filename

    def checkline(self, filename, lineno):
        line = linecache.getline(filename, lineno, _odb.getGlobals())
        if not line:
            print >>self.stdout, 'End of file'
            return 0
        line = line.strip()
        # Don't allow setting breakpoint at a blank line
        if (not line or (line[0] == '#') or
                (line[:3] == '"""') or line[:3] == "'''"):
            print >>self.stdout, '*** Blank or comment'
            return 0
        return lineno

    def lookupmodule(self, filename):
        """Helper function for break/clear parsing -- may be overridden.

        lookupmodule() translates (possibly incomplete) file or module name
        into an absolute file name.
        """
        if os.path.isabs(filename) and  os.path.exists(filename):
            return filename
        f = os.path.join(sys.path[0], filename)
        if  os.path.exists(f) and self.canonic(f) == self.mainpyfile:
            return f
        root, ext = os.path.splitext(filename)
        if ext == '':
            filename = filename + '.py'
        if os.path.isabs(filename):
            return filename
        for dirname in sys.path:
            while os.path.islink(dirname):
                dirname = os.readlink(dirname)
            fullname = os.path.join(dirname, filename)
            if os.path.exists(fullname):
                return fullname
        return None

    def do_clear(self, arg):
        """Three possibilities, tried in this order:
        clear -> clear all breaks, ask for confirmation
        clear file:lineno -> clear all breaks at file:lineno
        clear bpno bpno ... -> clear breakpoints by number"""
        if not arg:
            try:
                reply = raw_input('Clear all breaks? ')
            except EOFError:
                reply = 'no'
            reply = reply.strip().lower()
            if reply in ('y', 'yes'):
                _odb.clearAllBreakpoints()
            return
        if ':' in arg:
            # Make sure it works for "clear C:\foo\bar.py:12"
            i = arg.rfind(':')
            filename = arg[:i]
            arg = arg[i+1:]
            try:
                lineno = int(arg)
            except ValueError:
                err = "Invalid line number (%s)" % arg
            else:
                err = _odb.clearBreakpoint(filename, lineno)
            if err: print >>self.stdout, '***', err
            return
        numberlist = arg.split()
        for i in numberlist:
            try:
                i = int(i)
            except ValueError:
                print >>self.stdout, 'Breakpoint index %r is not a number' % i
                continue

            err = _odb.clearBreakpointNumber(i)
            if err:
                print >>self.stdout, '***', err
            else:
                print >>self.stdout, 'Deleted breakpoint', i
    do_cl = do_clear # 'c' is already an abbreviation for 'continue'

    def do_quit(self, arg):
        self.quit = 1

    def do_restart(self, arg):
        _odb.do_jump(0)
        return NAVIGATION_COMMAND_FLAG

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
    do_o = do_out
    do_c = do_continue
    do_rc = do_rcontinue

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
            event_lineno = _odb.getCurrentEventLineno()
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
                self.print_stack_entry(currentframe.filename, event_lineno, currentframe.name)

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
        sys.setodbtrace(self.trace_dispatch)
        if not isinstance(cmd, types.CodeType):
            cmd = cmd + '\n'
        try:
            oldstdout = sys.stdout
            sys.stdout = self.recorded_stdout = _odb.StringIO()
            exec cmd in globals, locals
        except Exception as e:
            print "Uncaught exception", e
            _odb.uncaughtExceptionEvent(e)
        finally:
            sys.setodbtrace(None)
            sys.stdout = oldstdout

        print "Program has finished, now entering ODB mode"
        _odb.setup()
        self.control_loop()
        _odb.reset()
        _odb.cleanupGlobals(__main__.__dict__)


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
