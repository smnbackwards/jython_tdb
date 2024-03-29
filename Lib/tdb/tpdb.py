#! /usr/bin/env python

"""A Timetravelling Python debugger.
    This code is based on pdb.py from the Python standard library
    The main modifications are to the treatment of navigation commands
        ie do_step, do_rstep
"""


import sys
import linecache
from repr import Repr
import os
import re
import pprint
import traceback

from .tbdb import Tbdb, Breakpoint
from .commandlinecontroller import CommandLineController


class Restart(Exception):
    """Causes a debugger to be restarted for the debugged python program."""
    pass

class ReExecute(Exception):
    """Causes a debugger to re-execute the debugged python program."""
    pass

# Create a custom safe Repr instance and increase its maxstring.
# The default of 30 truncates error messages too easily.
_repr = Repr()
_repr.maxstring = 200
_saferepr = _repr.repr

__all__ = ["Tpdb"]

def find_function(funcname, filename):
    cre = re.compile(r'def\s+%s\s*[(]' % re.escape(funcname))
    try:
        fp = open(filename)
    except IOError:
        return None
    # consumer of this info expects the first line to be 1
    lineno = 1
    answer = None
    while 1:
        line = fp.readline()
        if line == '':
            break
        if cre.match(line):
            answer = funcname, filename, lineno
            break
        lineno = lineno + 1
    fp.close()
    return answer


# Interaction prompt line will separate file and call info from code
# text using value of line_prefix string.  A newline and arrow may
# be to your liking.  You can set it once pdb is imported using the
# command "pdb.line_prefix = '\n% '".
# line_prefix = ': '    # Use this to get the old situation back
line_prefix = '\n-> '   # Probably a better default

class Tpdb(Tbdb):

    def __init__(self, controller = CommandLineController(), skip=None):
        Tbdb.__init__(self, skip=skip)
        self.controller = controller
        self.controller.debugger = self

        self.stdout = self.controller.stdout
        self.stdin = self.controller.stdin

        self.mainpyfile = ''
        self._user_requested_quit = 0

    #region state reseting
    def reset(self):
        Tbdb.reset(self)
        self.forget()

    def forget(self):
        self.lineno = None
        self.stack = []
        self.curindex = 0
        self.curframe = None

    def setup(self, f, t):
        self.forget()
        self.stack, self.curindex = self.get_stack(f, t)
        self.curframe = self.stack[self.curindex][0]
        # The f_locals dictionary is updated from the actual frame
        # locals whenever the .f_locals accessor is called, so we
        # cache it here to ensure that modifications are not overwritten.
        self.curframe_locals = self.curframe.f_locals
    #endregion

    # Override Bdb methods
    #region user trace methods
    def user_call(self, frame, ic, depth, argument_list):
        """This method is called when there is the remote possibility
        that we ever need to stop in this function."""
        print >>self.stdout, '--Call--'
        self.interaction(frame, None)

    def user_line(self, frame, ic, depth):
        """This function is called when we stop or break at this line."""
        self.interaction(frame, None)

    def user_return(self, frame, ic, depth, return_value):
        """This function is called when a return trap is set here."""
        frame.f_locals['__return__'] = return_value
        print >>self.stdout, '--Return--'
        self.interaction(frame, None)

    def user_exception(self, frame, ic, depth, exc_info):
        """This function is called if an exception occurs,
        but only if we are to stop at or just below this level."""
        exc_type, exc_value, exc_traceback = exc_info
        frame.f_locals['__exception__'] = exc_type, exc_value

        # Jython bubbles exceptions in a strange way
        # Without this if statement, the user would need to 'step' a number of times
        # to allow the exception to be propagated to the handler in mainloop()
        # this keeps 'interaction()' from being called, so the process is transparent to the user
        if exc_type == Restart:
            raise Restart
        elif exc_type == ReExecute:
            raise ReExecute
        elif exc_type == AssertionError:
            raise AssertionError(exc_value)

        if type(exc_type) == type(''):
            exc_type_name = exc_type
        else: exc_type_name = exc_type.__name__
        print >>self.stdout, exc_type_name + ':', _saferepr(exc_value)

        self.interaction(frame, exc_traceback)
    #endregion

    # General interaction function

    def interaction(self, frame, traceback):
        #if we have set a stop count and the current count is before it, then we are in replay mode
        if self.get_redomode() :
            return False

        self.setup(frame, traceback)
        self.print_stack_entry(self.stack[self.curindex])
        self.controller.get_cmd()
        self.forget()
        return True

    # Command definitions, called by cmdloop()
    # The argument is the remaining string on the command line
    # Return true to exit from the command loop

    #region Breakpoint commands
    def do_break(self, arg, temporary = 0):
        # break [ ([filename:]lineno | function) [, "condition"] ]
        if not arg:
            if self.breaks:  # There's at least one
                print >>self.stdout, "Num Type         Disp Enb   Where"
                for bp in Breakpoint.bpbynumber:
                    if bp:
                        bp.bpprint(self.stdout)
            return
        # parse arguments; comma has lowest precedence
        # and cannot occur in filename
        filename = None
        lineno = None
        cond = None
        comma = arg.find(',')
        if comma > 0:
            # parse stuff after comma: "condition"
            cond = arg[comma+1:].lstrip()
            arg = arg[:comma].rstrip()
        # parse stuff before comma: [filename:]lineno | function
        colon = arg.rfind(':')
        funcname = None
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
            # no colon; can be lineno or function
            try:
                lineno = int(arg)
            except ValueError:
                try:
                    func = eval(arg,
                                self.curframe.f_globals,
                                self.curframe_locals)
                except:
                    func = arg
                try:
                    if hasattr(func, 'im_func'):
                        func = func.im_func
                    code = func.func_code
                    #use co_name to identify the bkpt (function names
                    #could be aliased, but co_name is invariant)
                    funcname = code.co_name
                    lineno = code.co_firstlineno
                    filename = code.co_filename
                except:
                    # last thing to try
                    (ok, filename, ln) = self.lineinfo(arg)
                    if not ok:
                        print >>self.stdout, '*** The specified object',
                        print >>self.stdout, repr(arg),
                        print >>self.stdout, 'is not a function'
                        print >>self.stdout, 'or was not found along sys.path.'
                        return
                    funcname = ok # ok contains a function name
                    lineno = int(ln)
        if not filename:
            filename = self.defaultFile()
        # Check for reasonable breakpoint
        line = self.checkline(filename, lineno)
        if line:
            # now set the break point
            err = self.set_break(filename, line, temporary, cond, funcname)
            if err: print >>self.stdout, '***', err
            else:
                bp = self.get_breaks(filename, line)[-1]
                print >>self.stdout, "Breakpoint %d at %s:%d" % (bp.number,
                                                                 bp.file,
                                                                 bp.line)

    # To be overridden in derived debuggers

    def defaultFile(self):
        """Produce a reasonable default."""
        filename = self.curframe.f_code.co_filename
        if filename == '<string>' and self.mainpyfile:
            filename = self.mainpyfile
        return filename

    do_b = do_break

    def do_tbreak(self, arg):
        self.do_break(arg, 1)

    def lineinfo(self, identifier):
        failed = (None, None, None)
        # Input is identifier, may be in single quotes
        idstring = identifier.split("'")
        if len(idstring) == 1:
            # not in single quotes
            id = idstring[0].strip()
        elif len(idstring) == 3:
            # quoted
            id = idstring[1].strip()
        else:
            return failed
        if id == '': return failed
        parts = id.split('.')
        # Protection for derived debuggers
        if parts[0] == 'self':
            del parts[0]
            if len(parts) == 0:
                return failed
        # Best first guess at file to look at
        fname = self.defaultFile()
        if len(parts) == 1:
            item = parts[0]
        else:
            # More than one part.
            # First is module, second is method/class
            f = self.lookupmodule(parts[0])
            if f:
                fname = f
            item = parts[1]
        answer = find_function(item, fname)
        return answer or failed

    def checkline(self, filename, lineno):
        """Check whether specified line seems to be executable.

        Return `lineno` if it is, 0 if not (e.g. a docstring, comment, blank
        line or EOF). Warning: testing is not comprehensive.
        """
        # this method should be callable before starting debugging, so default
        # to "no globals" if there is no current frame
        globs = self.curframe.f_globals if hasattr(self, 'curframe') else None
        line = linecache.getline(filename, lineno, globs)
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

    def do_enable(self, arg):
        args = arg.split()
        for i in args:
            try:
                i = int(i)
            except ValueError:
                print >>self.stdout, 'Breakpoint index %r is not a number' % i
                continue

            if not (0 <= i < len(Breakpoint.bpbynumber)):
                print >>self.stdout, 'No breakpoint numbered', i
                continue

            bp = Breakpoint.bpbynumber[i]
            if bp:
                bp.enable()

    def do_disable(self, arg):
        args = arg.split()
        for i in args:
            try:
                i = int(i)
            except ValueError:
                print >>self.stdout, 'Breakpoint index %r is not a number' % i
                continue

            if not (0 <= i < len(Breakpoint.bpbynumber)):
                print >>self.stdout, 'No breakpoint numbered', i
                continue

            bp = Breakpoint.bpbynumber[i]
            if bp:
                bp.disable()

    def do_condition(self, arg):
        # arg is breakpoint number and condition
        args = arg.split(' ', 1)
        try:
            bpnum = int(args[0].strip())
        except ValueError:
            # something went wrong
            print >>self.stdout, \
                'Breakpoint index %r is not a number' % args[0]
            return
        try:
            cond = args[1]
        except:
            cond = None
        try:
            bp = Breakpoint.bpbynumber[bpnum]
        except IndexError:
            print >>self.stdout, 'Breakpoint index %r is not valid' % args[0]
            return
        if bp:
            bp.cond = cond
            if not cond:
                print >>self.stdout, 'Breakpoint', bpnum,
                print >>self.stdout, 'is now unconditional.'

    def do_ignore(self,arg):
        """arg is bp number followed by ignore count."""
        args = arg.split()
        try:
            bpnum = int(args[0].strip())
        except ValueError:
            # something went wrong
            print >>self.stdout, \
                'Breakpoint index %r is not a number' % args[0]
            return
        try:
            count = int(args[1].strip())
        except:
            count = 0
        try:
            bp = Breakpoint.bpbynumber[bpnum]
        except IndexError:
            print >>self.stdout, 'Breakpoint index %r is not valid' % args[0]
            return
        if bp:
            bp.ignore = count
            if count > 0:
                reply = 'Will ignore next '
                if count > 1:
                    reply = reply + '%d crossings' % count
                else:
                    reply = reply + '1 crossing'
                print >>self.stdout, reply + ' of breakpoint %d.' % bpnum
            else:
                print >>self.stdout, 'Will stop next time breakpoint',
                print >>self.stdout, bpnum, 'is reached.'

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
                self.clear_all_breaks()
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
                err = self.clear_break(filename, lineno)
            if err: print >>self.stdout, '***', err
            return
        numberlist = arg.split()
        for i in numberlist:
            try:
                i = int(i)
            except ValueError:
                print >>self.stdout, 'Breakpoint index %r is not a number' % i
                continue

            if not (0 <= i < len(Breakpoint.bpbynumber)):
                print >>self.stdout, 'No breakpoint numbered', i
                continue
            err = self.clear_bpbynumber(i)
            if err:
                print >>self.stdout, '***', err
            else:
                print >>self.stdout, 'Deleted breakpoint', i
    do_cl = do_clear # 'c' is already an abbreviation for 'continue'
    #endregion

    #region Stack commands
    def do_where(self, arg):
        self.print_stack_trace()
    do_w = do_where
    do_bt = do_where

    def do_up(self, arg):
        if self.curindex == 0:
            print >>self.stdout, '*** Oldest frame'
        else:
            self.curindex = self.curindex - 1
            self.curframe = self.stack[self.curindex][0]
            self.curframe_locals = self.curframe.f_locals
            self.print_stack_entry(self.stack[self.curindex])
            self.lineno = None
    do_u = do_up

    def do_down(self, arg):
        if self.curindex + 1 == len(self.stack):
            print >>self.stdout, '*** Newest frame'
        else:
            self.curindex = self.curindex + 1
            self.curframe = self.stack[self.curindex][0]
            self.curframe_locals = self.curframe.f_locals
            self.print_stack_entry(self.stack[self.curindex])
            self.lineno = None
    do_d = do_down
    #endregion

    #region Navigation Commands
    def do_step(self, arg):
        self.set_step()
        return 1
    do_s = do_step

    def do_next(self, arg):
        self.set_next(self.curframe)
        return 1
    do_n = do_next

    def do_run(self, arg):
        """Restart program by raising an exception to be caught in the main
        debugger loop.  If arguments were given, set them in sys.argv."""
        if arg:
            import shlex
            argv0 = sys.argv[0:1]
            sys.argv = shlex.split(arg)
            sys.argv[:0] = argv0
        #todo added this?

        raise Restart

    do_restart = do_run

    def do_return(self, arg):
        self.set_return(self.curframe)
        return 1
    do_r = do_return

    def do_continue(self, arg):
        self.set_continue()
        return 1
    do_c = do_cont = do_continue
    #endregion

    #region Reverse Navigation commands
    def re_execute(self):
        print "Stepping back to instruction", -1 #todo self.stopic
        try:
            self.do_restart(None)
        except Restart:
            raise ReExecute

    def do_jump(self, arg):
        if arg :
            args = arg.split()
            try:
                step_to = int(args[0].strip())
            except ValueError:
                # something went wrong
                print >>self.stdout, 'jump argument must be an int >= 0'
                return
            if step_to < 0:
                print >> self.stdout, 'jump %r must be an int >= 0' % step_to
                return
            if self.get_ic() == step_to:
                return
            self.set_jump(step_to)
            if step_to < self.get_ic() :
                self.re_execute()
            return 1


        else :
            print >>self.stdout, 'jump requires an instruction count to jump to'

    def do_rstep(self, arg):
        self.set_rstep()
        self.re_execute()

    def do_rreturn(self, arg):
        self.set_rreturn()
        self.re_execute()

    def do_rnext(self, arg):
        self.set_rnext()
        self.re_execute()

    #endregion

    #region End Commands
    def do_quit(self, arg):
        self._user_requested_quit = 1
        self.set_quit()
        return 1

    do_q = do_quit
    do_exit = do_quit

    def do_EOF(self, arg):
        print >>self.stdout
        self._user_requested_quit = 1
        self.set_quit()
        return 1
    #endregion

    #region Information Commands
    def do_args(self, arg):
        co = self.curframe.f_code
        dict = self.curframe_locals
        n = co.co_argcount
        if co.co_flags & 4: n = n+1
        if co.co_flags & 8: n = n+1
        for i in range(n):
            name = co.co_varnames[i]
            print >>self.stdout, name, '=',
            if name in dict: print >>self.stdout, dict[name]
            else: print >>self.stdout, "*** undefined ***"
    do_a = do_args

    def do_retval(self, arg):
        if '__return__' in self.curframe_locals:
            print >>self.stdout, self.curframe_locals['__return__']
        else:
            print >>self.stdout, '*** Not yet returned!'
    do_rv = do_retval

    def _getval(self, arg):
        try:
            return eval(arg, self.curframe.f_globals,
                        self.curframe_locals)
        except:
            t, v = sys.exc_info()[:2]
            if isinstance(t, str):
                exc_type_name = t
            else: exc_type_name = t.__name__
            print >>self.stdout, '***', exc_type_name + ':', repr(v)
            raise

    def do_p(self, arg):
        try:
            print >>self.stdout, repr(self._getval(arg))
        except:
            pass

    def do_pp(self, arg):
        try:
            pprint.pprint(self._getval(arg), self.stdout)
        except:
            pass

    def do_list(self, arg):
        self.lastcmd = 'list'
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
                print >>self.stdout, '*** Error in argument:', repr(arg)
                return
        elif self.lineno is None:
            first = max(1, self.curframe.f_lineno - 5)
        else:
            first = self.lineno + 1
        if last is None:
            last = first + 10
        filename = self.curframe.f_code.co_filename
        breaklist = self.get_file_breaks(filename)
        try:
            for lineno in range(first, last+1):
                line = linecache.getline(filename, lineno,
                                         self.curframe.f_globals)
                if not line:
                    print >>self.stdout, '[EOF]'
                    break
                else:
                    s = repr(lineno).rjust(3)
                    if len(s) < 4: s = s + ' '
                    if lineno in breaklist: s = s + 'B'
                    else: s = s + ' '
                    if lineno == self.curframe.f_lineno:
                        s = s + '->'
                    print >>self.stdout, s + '\t' + line,
                    self.lineno = lineno
        except KeyboardInterrupt:
            pass
    do_l = do_list

    def do_whatis(self, arg):
        try:
            value = eval(arg, self.curframe.f_globals,
                         self.curframe_locals)
        except:
            t, v = sys.exc_info()[:2]
            if type(t) == type(''):
                exc_type_name = t
            else: exc_type_name = t.__name__
            print >>self.stdout, '***', exc_type_name + ':', repr(v)
            return
        code = None
        # Is it a function?
        try: code = value.func_code
        except: pass
        if code:
            print >>self.stdout, 'Function', code.co_name
            return
        # Is it an instance method?
        try: code = value.im_func.func_code
        except: pass
        if code:
            print >>self.stdout, 'Method', code.co_name
            return
        # None of the above...
        print >>self.stdout, type(value)
    #endregion

    # Print a traceback starting at the top stack frame.
    # The most recently entered frame is printed last;
    # this is different from dbx and gdb, but consistent with
    # the Python interpreter's stack trace.
    # It is also consistent with the up/down commands (which are
    # compatible with dbx and gdb: up moves towards 'main()'
    # and down moves towards the most recent stack frame).

    def print_stack_trace(self):
        try:
            for frame_lineno in self.stack:
                self.print_stack_entry(frame_lineno)
        except KeyboardInterrupt:
            pass

    def print_stack_entry(self, frame_lineno, prompt_prefix=line_prefix):
        frame, lineno = frame_lineno
        if frame is self.curframe:
            print >>self.stdout, '>',
        else:
            print >>self.stdout, ' ',
        print >>self.stdout, self.format_stack_entry(frame_lineno,
                                                     prompt_prefix)


    # Help methods
    #region Help

    def help_help(self):
        self.help_h()

    def help_h(self):
        print >>self.stdout, """h(elp)
Without argument, print the list of available commands.
With a command name as argument, print help about that command
"help pdb" pipes the full documentation file to the $PAGER
"help exec" gives help on the ! command"""

    def help_where(self):
        self.help_w()

    def help_w(self):
        print >>self.stdout, """w(here)
Print a stack trace, with the most recent frame at the bottom.
An arrow indicates the "current frame", which determines the
context of most commands.  'bt' is an alias for this command."""

    help_bt = help_w

    def help_down(self):
        self.help_d()

    def help_d(self):
        print >>self.stdout, """d(own)
Move the current frame one level down in the stack trace
(to a newer frame)."""

    def help_up(self):
        self.help_u()

    def help_u(self):
        print >>self.stdout, """u(p)
Move the current frame one level up in the stack trace
(to an older frame)."""

    def help_break(self):
        self.help_b()

    def help_b(self):
        print >>self.stdout, """b(reak) ([file:]lineno | function) [, condition]
With a line number argument, set a break there in the current
file.  With a function name, set a break at first executable line
of that function.  Without argument, list all breaks.  If a second
argument is present, it is a string specifying an expression
which must evaluate to true before the breakpoint is honored.

The line number may be prefixed with a filename and a colon,
to specify a breakpoint in another file (probably one that
hasn't been loaded yet).  The file is searched for on sys.path;
the .py suffix may be omitted."""

    def help_clear(self):
        self.help_cl()

    def help_cl(self):
        print >>self.stdout, "cl(ear) filename:lineno"
        print >>self.stdout, """cl(ear) [bpnumber [bpnumber...]]
With a space separated list of breakpoint numbers, clear
those breakpoints.  Without argument, clear all breaks (but
first ask confirmation).  With a filename:lineno argument,
clear all breaks at that line in that file.

Note that the argument is different from previous versions of
the debugger (in python distributions 1.5.1 and before) where
a linenumber was used instead of either filename:lineno or
breakpoint numbers."""

    def help_tbreak(self):
        print >>self.stdout, """tbreak  same arguments as break, but breakpoint
is removed when first hit."""

    def help_enable(self):
        print >>self.stdout, """enable bpnumber [bpnumber ...]
Enables the breakpoints given as a space separated list of
bp numbers."""

    def help_disable(self):
        print >>self.stdout, """disable bpnumber [bpnumber ...]
Disables the breakpoints given as a space separated list of
bp numbers."""

    def help_ignore(self):
        print >>self.stdout, """ignore bpnumber count
Sets the ignore count for the given breakpoint number.  A breakpoint
becomes active when the ignore count is zero.  When non-zero, the
count is decremented each time the breakpoint is reached and the
breakpoint is not disabled and any associated condition evaluates
to true."""

    def help_condition(self):
        print >>self.stdout, """condition bpnumber str_condition
str_condition is a string specifying an expression which
must evaluate to true before the breakpoint is honored.
If str_condition is absent, any existing condition is removed;
i.e., the breakpoint is made unconditional."""

    def help_step(self):
        self.help_s()

    def help_s(self):
        print >>self.stdout, """s(tep)
Execute the current line, stop at the first possible occasion
(either in a function that is called or in the current function)."""

    def help_until(self):
        self.help_unt()

    def help_unt(self):
        print """unt(il)
Continue execution until the line with a number greater than the current
one is reached or until the current frame returns"""

    def help_next(self):
        self.help_n()

    def help_n(self):
        print >>self.stdout, """n(ext)
Continue execution until the next line in the current function
is reached or it returns."""

    def help_return(self):
        self.help_r()

    def help_r(self):
        print >>self.stdout, """r(eturn)
Continue execution until the current function returns."""

    def help_continue(self):
        self.help_c()

    def help_cont(self):
        self.help_c()

    def help_c(self):
        print >>self.stdout, """c(ont(inue))
Continue execution, only stop when a breakpoint is encountered."""

    def help_list(self):
        self.help_l()

    def help_l(self):
        print >>self.stdout, """l(ist) [first [,last]]
List source code for the current file.
Without arguments, list 11 lines around the current line
or continue the previous listing.
With one argument, list 11 lines starting at that line.
With two arguments, list the given range;
if the second argument is less than the first, it is a count."""

    def help_args(self):
        self.help_a()

    def help_a(self):
        print >>self.stdout, """a(rgs)
Print the arguments of the current function."""

    def help_p(self):
        print >>self.stdout, """p expression
Print the value of the expression."""

    def help_pp(self):
        print >>self.stdout, """pp expression
Pretty-print the value of the expression."""

    def help_run(self):
        print """run [args...]
Restart the debugged python program. If a string is supplied, it is
splitted with "shlex" and the result is used as the new sys.argv.
History, breakpoints, actions and debugger options are preserved.
"restart" is an alias for "run"."""

    help_restart = help_run

    def help_quit(self):
        self.help_q()

    def help_q(self):
        print >>self.stdout, """q(uit) or exit - Quit from the debugger.
The program being executed is aborted."""

    help_exit = help_q

    def help_whatis(self):
        print >>self.stdout, """whatis arg
Prints the type of the argument."""

    def help_EOF(self):
        print >>self.stdout, """EOF
Handles the receipt of EOF as a command."""
    #endregion

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

    def _runscript(self, filename):
        # The script has to run in __main__ namespace (or imports from
        # __main__ will break).
        #
        # So we clear up the __main__ and set several special variables
        # (this gets rid of tpdb's globals and cleans old variables on restarts).
        import __main__
        __main__.__dict__.clear()
        __main__.__dict__.update({"__name__"    : "__main__",
                                  "__file__"    : filename,
                                  "__builtins__": __builtins__,
                                  })
        # When tbdb sets tracing, a number of call and line events happens
        # BEFORE debugger even reaches user's code (and the exact sequence of
        # events depends on python version). So we take special measures to
        # avoid stopping before we reach the main script (see user_line and
        # user_call for details).
        self.mainpyfile = self.canonic(filename)
        # self._user_requested_quit = 0
        statement = 'execfile(%r)' % filename
        self.run(statement)

def mainloop(tpdb, mainpyfile):
    while True:
        try:
            tpdb._runscript(mainpyfile)
            if tpdb._user_requested_quit:
                break
            print "The program finished after executing", tpdb.get_ic() , "instructions and will be restarted"
        except ReExecute:
            print "Re-executing from beginning"
            tpdb.enable_redomode()
            pass
        except Restart:
            print "Restarting", mainpyfile, "with arguments:"
            print "\t" + " ".join(sys.argv[1:])
        except SystemExit:
            # In most cases SystemExit does not warrant a post-mortem session.
            print "The program exited via sys.exit(). Exit status: ",
            print sys.exc_info()[1]
        except:
            traceback.print_exc()
            print "Uncaught exception. Entering post mortem debugging"
            print "Running 'cont' or 'step' will restart the program"
            t = sys.exc_info()[2]
            tpdb.interaction(None, t)
            print "Post mortem debugger finished. The " + mainpyfile + \
                  " will be restarted"

def main():
    if not sys.argv[1:] or sys.argv[1] in ("--help", "-h"):
        print "usage: tpdb.py scriptfile [arg] ..."
        sys.exit(2)

    mainpyfile =  sys.argv[1]     # Get script filename
    if not os.path.exists(mainpyfile):
        print 'Error:', mainpyfile, 'does not exist'
        sys.exit(1)

    del sys.argv[0]         # Hide "tpdb.py" from argument list

    # Replace tpdb's dir with script's dir in front of module search path.
    sys.path[0] = os.path.dirname(mainpyfile)

    # Note on saving/restoring sys.argv: it's a good idea when sys.argv was
    # modified by the script being debugged. It's a bad idea when it was
    # changed by the user from the command line. There is a "restart" command
    # which allows explicit specification of command line arguments.
    tpdb = Tpdb()
    mainloop(tpdb,mainpyfile)


# When invoked as main program, invoke the debugger on a script
if __name__ == '__main__':
    import tdb.tpdb
    tdb.tpdb.main()
