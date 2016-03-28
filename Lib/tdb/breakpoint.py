import sys


class Breakpoint:
    """Breakpoint class

    Implements temporary breakpoints, ignore counts, disabling and
    (re)-enabling, and conditionals.

    Breakpoints are indexed by number through bpbynumber and by
    the file,line tuple using bplist.  The former points to a
    single instance of class Breakpoint.  The latter points to a
    list of such instances since there may be more than one
    breakpoint per line.

    """

    # XXX Keeping state in the class is a mistake -- this means
    # you cannot have more than one active Bdb instance.

    next = 1  # Next bp to be assigned
    bplist = {}  # indexed by (file, lineno) tuple
    bpbynumber = [None]  # Each entry is None or an instance of Bpt

    # index 0 is unused, except for marking an
    # effective break .... see effective()

    def __init__(self, file, line, temporary=0, cond=None, funcname=None):
        self.funcname = funcname
        # Needed if funcname is not None.
        self.func_first_executable_line = None
        self.file = file  # This better be in canonical form!
        self.line = line
        self.temporary = temporary
        self.cond = cond
        self.enabled = 1
        self.ignore = 0
        self.hits = 0
        self.number = Breakpoint.next
        Breakpoint.next = Breakpoint.next + 1
        # Build the two lists
        self.bpbynumber.append(self)
        if (file, line) in self.bplist:
            self.bplist[file, line].append(self)
        else:
            self.bplist[file, line] = [self]

    def deleteMe(self):
        index = (self.file, self.line)
        self.bpbynumber[self.number] = None  # No longer in list
        self.bplist[index].remove(self)
        if not self.bplist[index]:
            # No more bp for this f:l combo
            del self.bplist[index]

    def enable(self):
        self.enabled = 1

    def disable(self):
        self.enabled = 0

    def bpprint(self, out=None):
        if out is None:
            out = sys.stdout
        if self.temporary:
            disp = 'del  '
        else:
            disp = 'keep '
        if self.enabled:
            disp = disp + 'yes  '
        else:
            disp = disp + 'no   '
        print >> out, '%-4dbreakpoint   %s at %s:%d' % (self.number, disp,
                                                        self.file, self.line)
        if self.cond:
            print >> out, '\tstop only if %s' % (self.cond,)
        if self.ignore:
            print >> out, '\tignore next %d hits' % (self.ignore)
        if (self.hits):
            if (self.hits > 1):
                ss = 's'
            else:
                ss = ''
            print >> out, ('\tbreakpoint already hit %d time%s' %
                           (self.hits, ss))


# -----------end of Breakpoint class----------

def checkfuncname(b, frame):
    """Check whether we should break here because of `b.funcname`."""
    if not b.funcname:
        # Breakpoint was set via line number.
        if b.line != frame.f_lineno:
            # Breakpoint was set at a line with a def statement and the function
            # defined is called: don't break.
            return False
        return True

    # Breakpoint set via function name.

    if frame.f_code.co_name != b.funcname:
        # It's not a function call, but rather execution of def statement.
        return False

    # We are in the right frame.
    if not b.func_first_executable_line:
        # The function is entered for the 1st time.
        b.func_first_executable_line = frame.f_lineno

    if b.func_first_executable_line != frame.f_lineno:
        # But we are not at the first line number: don't break.
        return False
    return True


# Determines if there is an effective (active) breakpoint at this
# line of code.  Returns breakpoint number or 0 if none
def effective(file, line, frame):
    """Determine which breakpoint for this file:line is to be acted upon.

    Called only if we know there is a bpt at this
    location.  Returns breakpoint that was triggered and a flag
    that indicates if it is ok to delete a temporary bp.

    """
    possibles = Breakpoint.bplist[file, line]
    for i in range(0, len(possibles)):
        b = possibles[i]
        if b.enabled == 0:
            continue
        if not checkfuncname(b, frame):
            continue
        # Count every hit when bp is enabled
        b.hits = b.hits + 1
        if not b.cond:
            # If unconditional, and ignoring,
            # go on to next, else break
            if b.ignore > 0:
                b.ignore = b.ignore - 1
                continue
            else:
                # breakpoint and marker that's ok
                # to delete if temporary
                return (b, 1)
        else:
            # Conditional bp.
            # Ignore count applies only to those bpt hits where the
            # condition evaluates to true.
            try:
                val = eval(b.cond, frame.f_globals,
                           frame.f_locals)
                if val:
                    if b.ignore > 0:
                        b.ignore = b.ignore - 1
                        # continue
                    else:
                        return (b, 1)
                        # else:
                        #   continue
            except:
                # if eval fails, most conservative
                # thing is to stop on breakpoint
                # regardless of ignore count.
                # Don't delete temporary,
                # as another hint to user.
                return (b, 0)
    return (None, None)