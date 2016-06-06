'''
The base debugger for Tdb
This code is a modified version of 'bdb.py'
The majority of modifications are found in the handling of navigation
    ie 'trace_distpatch', 'dispatch_' methods, 'set_' methods and 'stop_here'
'''
import fnmatch
import sys
import os
import types
import _tdb
from .breakpoint import Breakpoint, effective, checkfuncname

__all__ = ["BdbQuit","Tbdb"]

class BdbQuit(Exception):
    """Exception to give up completely"""

debug_enabled = 0
# debug_enabled = 1
def debug(output):
    if debug_enabled :
        print >> sys.stderr, output


class Tbdb:
    def __init__(self, skip=None):
        self.skip = set(skip) if skip else None
        self.breaks = {}
        self.fncache = {}

        # The instruction count to stop at
        self.stopic = -1

        #The call depth to stop at
        self.stopdepth = -1

        #Whether the debugger is re-executing
        self.redomode = False

        self.quitting = 0


    def canonic(self, filename):
        if filename == "<" + filename[1:-1] + ">":
            return filename
        canonic = self.fncache.get(filename)
        if not canonic:
            canonic = os.path.abspath(filename)
            canonic = os.path.normcase(canonic)
            self.fncache[filename] = canonic
        return canonic

    def reset(self):
        import linecache
        linecache.checkcache()
        if not self.redomode:
            # Reset the stop values to stop at instruction count 0 at any depth
            # If we are re-executing values have already been set
            self._set_stopinfo(0, -1)
        _tdb.reset_instruction_count()
        self.quitting = 0

    def trace_start(self, frame, event, arg):
        self.botframe = frame
        sys.settdbtrace(self.trace_dispatch)
        return self.trace_dispatch(frame,event,arg)

    def trace_dispatch(self, frame, event, arg):
        if self.quitting:
            return  # None

        ic = self.get_ic()
        depth = self.get_depth()
        debug("%s event at %i %i"%(event,ic,depth))

        self.event = event
        if event == 'line':
            self.dispatch_line(frame, ic, depth)
        elif event == 'call':
            self.dispatch_call(frame, ic, depth, arg)
        elif event == 'return':
            self.dispatch_return(frame, ic, depth, arg)
            if self.get_depth() == 0 :
                sys.settdbtrace(None)
                return None
        elif event == 'exception':
            self.dispatch_exception(frame, ic, depth, arg)
        else:
            print 'idb: unknown debugging event:', repr(event)

        self.last_event = event
        if self.quitting: raise BdbQuit

        return self.trace_dispatch

    def dispatch_line(self, frame, ic, depth):
        if self.stop_here(frame, ic, depth) or self.break_here(frame):
            self.user_line(frame, ic, depth)

    def dispatch_call(self, frame, ic, depth, arg):
        if self.stop_here(frame,ic,depth):
            self.user_call(frame, ic, depth, arg)

    def dispatch_return(self, frame, ic, depth, arg):
        if self.stop_here(frame, ic, depth):
            self.user_return(frame, ic, depth, arg)

    def dispatch_exception(self, frame, ic, depth, arg):
        if self.stop_here(frame, ic, depth):
            self.user_exception(frame, ic, depth, arg)

    # Normally derived classes don't override the following
    # methods, but they may if they want to redefine the
    # definition of stopping and breakpoints.

    def is_skipped_module(self, module_name):
        for pattern in self.skip:
            if fnmatch.fnmatch(module_name, pattern):
                return True
        return False

    def stop_here(self, frame, ic, depth):
        if self.skip and \
                self.is_skipped_module(frame.f_globals.get('__name__')):
            return False

        if self.stopic >= 0 and ic >= self.stopic :
            if self.stopdepth == -1 or self.stopdepth >= depth :
                if not self.stopevent or self.event == self.stopevent:
                    debug("Stop  at %s @ %s True \t Actual: %s @ %s" % (self.stopic, self.stopdepth, ic, depth))
                    self.redomode = False
                    return True

        debug("Stop  at %s @ %s False \t Actual: %s @ %s" % (self.stopic, self.stopdepth, ic, depth))
        return False

    def break_here(self, frame):
        filename = self.canonic(frame.f_code.co_filename)
        if not filename in self.breaks:
            return False
        lineno = frame.f_lineno
        if not lineno in self.breaks[filename]:
            # The line itself has no breakpoint, but maybe the line is the
            # first line of a function with breakpoint set by function name.
            lineno = frame.f_code.co_firstlineno
            if not lineno in self.breaks[filename]:
                return False

        # flag says ok to delete temp. bp
        (bp, flag) = effective(filename, lineno, frame)
        if bp:
            self.currentbp = bp.number
            if (flag and bp.temporary):
                self.do_clear(str(bp.number))
            return True
        else:
            return False

    def do_clear(self, arg):
        raise NotImplementedError, "subclass of bdb must implement do_clear()"

    def break_anywhere(self, frame):
        return self.canonic(frame.f_code.co_filename) in self.breaks

    # Derived classes should override the user_* methods
    # to gain control.
    def user_call(self, frame, ic, depth, argument_list):
        """This method is called when there is the remote possibility
        that we ever need to stop in this function."""
        pass

    def user_line(self, frame, ic, depth):
        """This method is called when we stop or break at this line."""
        pass

    def user_return(self, frame, ic, depth, return_value):
        """This method is called when a return trap is set here."""
        pass

    def user_exception(self, frame, ic, depth, exc_info):
        exc_type, exc_value, exc_traceback = exc_info
        """This method is called if an exception occurs,
        but only if we are to stop at or just below this level."""
        pass

    def get_ic(self):
        return _tdb.instruction_count()

    def get_depth(self):
        return _tdb.call_depth()

    def get_last_call_instruction(self):
        ic = _tdb.get_last_call_instruction()
        assert ic >= 0
        return ic

    def get_previous_call_instruction(self):
        return _tdb.get_previous_call_instruction()

    def _set_stopinfo(self, stopic, stopdepth, stopevent=None):
        debug("Stop info set at %s %s"%(stopic, stopdepth))
        self.stopic = stopic
        self.stopdepth = stopdepth
        self.stopevent = stopevent
        # make sure the debugger doesn't try to exit since we now want to stop somewhere
        self.quitting = 0

    # Derived classes and clients can call the following methods
    # to affect the stepping state.

    def set_jump(self, ic):
        """Stop at next instruction at any depth"""
        self._set_stopinfo(ic, -1)

    def set_step(self):
        """Stop at next instruction at any depth"""
        self._set_stopinfo(self.get_ic() + 1, -1)

    def set_next(self, frame):
        """Stop on the next line in or below the current depth"""
        self._set_stopinfo(self.get_ic() + 1, self.get_depth())

    def set_return(self, frame):
        """Stop when returning from the given frame."""
        #if we just executed a Call isntruction, we return up one level
        event = None if self.event == 'return' else 'return'
        self._set_stopinfo(self.get_ic() + 1, self.get_depth(), event)

    def set_rstep(self, n):
        if n < 0:
            self._set_stopinfo(self.get_ic() + n, -1)
        else :
            self._set_stopinfo(n, -1)

    def set_rreturn(self):
        ic = self.get_ic() - 1 if self.event == 'call' else self.get_previous_call_instruction()
        self._set_stopinfo(max(ic, 0), -1)

    def set_rnext(self):
        if self.last_event == 'return':
            self._set_stopinfo(max(self.get_last_call_instruction() - 1, 0), -1)
        else :
            self.set_rstep(-1)

    def set_continue(self):
        # Don't stop except at breakpoints or when finished
        self._set_stopinfo(-1, -1)

    def set_quit(self):
        self.quitting = 1
        sys.settdbtrace(None)

    # Derived classes and clients can call the following methods
    # to manipulate breakpoints.  These methods return an
    # error message is something went wrong, None if all is well.
    # Set_break prints out the breakpoint line and file:lineno.
    # Call self.get_*break*() to see the breakpoints or better
    # for bp in Breakpoint.bpbynumber: if bp: bp.bpprint().

    def set_break(self, filename, lineno, temporary=0, cond = None,
                  funcname=None):
        filename = self.canonic(filename)
        import linecache # Import as late as possible
        line = linecache.getline(filename, lineno)
        if not line:
            return 'Line %s:%d does not exist' % (filename,
                                                  lineno)
        if not filename in self.breaks:
            self.breaks[filename] = []
        list = self.breaks[filename]
        if not lineno in list:
            list.append(lineno)
        bp = Breakpoint(filename, lineno, temporary, cond, funcname)

    def _prune_breaks(self, filename, lineno):
        if (filename, lineno) not in Breakpoint.bplist:
            self.breaks[filename].remove(lineno)
        if not self.breaks[filename]:
            del self.breaks[filename]

    def clear_break(self, filename, lineno):
        filename = self.canonic(filename)
        if not filename in self.breaks:
            return 'There are no breakpoints in %s' % filename
        if lineno not in self.breaks[filename]:
            return 'There is no breakpoint at %s:%d' % (filename,
                                                        lineno)
        # If there's only one bp in the list for that file,line
        # pair, then remove the breaks entry
        for bp in Breakpoint.bplist[filename, lineno][:]:
            bp.deleteMe()
        self._prune_breaks(filename, lineno)

    def clear_bpbynumber(self, arg):
        try:
            number = int(arg)
        except:
            return 'Non-numeric breakpoint number (%s)' % arg
        try:
            bp = Breakpoint.bpbynumber[number]
        except IndexError:
            return 'Breakpoint number (%d) out of range' % number
        if not bp:
            return 'Breakpoint (%d) already deleted' % number
        bp.deleteMe()
        self._prune_breaks(bp.file, bp.line)

    def clear_all_file_breaks(self, filename):
        filename = self.canonic(filename)
        if not filename in self.breaks:
            return 'There are no breakpoints in %s' % filename
        for line in self.breaks[filename]:
            blist = Breakpoint.bplist[filename, line]
            for bp in blist:
                bp.deleteMe()
        del self.breaks[filename]

    def clear_all_breaks(self):
        if not self.breaks:
            return 'There are no breakpoints'
        for bp in Breakpoint.bpbynumber:
            if bp:
                bp.deleteMe()
        self.breaks = {}

    def get_break(self, filename, lineno):
        filename = self.canonic(filename)
        return filename in self.breaks and \
               lineno in self.breaks[filename]

    def get_breaks(self, filename, lineno):
        filename = self.canonic(filename)
        return filename in self.breaks and \
               lineno in self.breaks[filename] and \
               Breakpoint.bplist[filename, lineno] or []

    def get_file_breaks(self, filename):
        filename = self.canonic(filename)
        if filename in self.breaks:
            return self.breaks[filename]
        else:
            return []

    def get_all_breaks(self):
        return self.breaks

    # Derived classes and clients can call the following method
    # to get a data structure representing a stack trace.

    def get_stack(self, f, t):
        stack = []
        if t and t.tb_frame is f:
            t = t.tb_next
        while f is not None:
            stack.append((f, f.f_lineno))
            if f is self.botframe:
                break
            f = f.f_back
        stack.reverse()
        i = max(0, len(stack) - 1)
        while t is not None:
            stack.append((t.tb_frame, t.tb_lineno))
            t = t.tb_next
        if f is None:
            i = max(0, len(stack) - 1)
        return stack, i

    #

    def format_stack_entry(self, frame_lineno, lprefix=': '):
        import linecache, repr
        frame, lineno = frame_lineno
        filename = self.canonic(frame.f_code.co_filename)
        s = '%s(%r)' % (filename, lineno)
        if frame.f_code.co_name:
            s = s + frame.f_code.co_name
        else:
            s = s + "<lambda>"
        if '__args__' in frame.f_locals:
            args = frame.f_locals['__args__']
        else:
            args = None
        if args:
            s = s + repr.repr(args)
        else:
            s = s + '()'
        if '__return__' in frame.f_locals:
            rv = frame.f_locals['__return__']
            s = s + '->'
            s = s + repr.repr(rv)
        line = linecache.getline(filename, lineno, frame.f_globals)
        if line: s = s + lprefix + line.strip()
        return s

    # The following method can be called by clients to use
    # a debugger to debug a statement, given as a string.

    def run(self, cmd, globals=None, locals=None):
        if globals is None:
            import __main__
            globals = __main__.__dict__
        if locals is None:
            locals = globals
        self.reset()
        sys.settdbtrace(self.trace_start)
        if not isinstance(cmd, types.CodeType):
            cmd = cmd+'\n'
        try:
            exec cmd in globals, locals
        except BdbQuit:
            pass
        finally:
            self.quitting = 1
            sys.settdbtrace(None)