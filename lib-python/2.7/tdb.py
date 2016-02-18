import os
import pdb
import sys
import traceback
from bdb import BdbQuit, Bdb

from cmd import Cmd
from pdb import Restart
import _tdb

class ReExecute(Exception):
    """Causes a debugger to be restarted for the debugged python program."""
    pass


def set_trace():
    Tdb().set_trace(sys._getframe().f_back)

def run(statement, globals=None, locals=None):
    Tdb().run(statement, globals, locals)

def instruction_count():
    return _tdb.instruction_count()



class Tdb(pdb.Pdb):
    def __init__(self, completekey='tab', stdin=None, stdout=None, skip=None):
        pdb.Pdb.__init__(self, completekey, stdin, stdout, skip)
        self.prompt = '(Tdb )'
        self.stop_ic = -1

    def preloop(self):
        Cmd.preloop(self)
        self.prompt = "(TDB) <%d>"%(instruction_count())

    def do_rstep(self, arg):
        if arg :
            args = arg.split()
            try:
                step_to = int(args[0].strip())
            except ValueError:
                # something went wrong
                print >>self.stdout, 'Rstep argument must be an int >= 0'
                return
            if step_to <= 0:
                print >> self.stdout, 'Rstep %r must be an int >= 0' % step_to
                return
        else :
            step_to = instruction_count() - 1

        if step_to >= 0:
            self.stop_ic = step_to
            print "Stepping back to instruction", self.stop_ic
            try:
                self.do_restart(None)
            except Restart:
                raise ReExecute
        else :
            print "Can't step to instruction %s because it is negative"%(step_to)

    def interaction(self, frame, traceback):
        if self.stop_ic >= 0 and instruction_count() < self.stop_ic :
            return

        if(self.stop_ic == instruction_count()) :
            self.stop_ic = -1
        pdb.Pdb.interaction(self, frame, traceback)

def main():
    if not sys.argv[1:] or sys.argv[1] in ("--help", "-h"):
        print "usage: tdb.py scriptfile [arg] ..."
        sys.exit(2)

    mainpyfile =  sys.argv[1]     # Get script filename
    if not os.path.exists(mainpyfile):
        print 'Error:', mainpyfile, 'does not exist'
        sys.exit(1)

    del sys.argv[0]         # Hide "pdb.py" from argument list

    # Replace pdb's dir with script's dir in front of module search path.
    sys.path[0] = os.path.dirname(mainpyfile)

    # Note on saving/restoring sys.argv: it's a good idea when sys.argv was
    # modified by the script being debugged. It's a bad idea when it was
    # changed by the user from the command line. There is a "restart" command
    # which allows explicit specification of command line arguments.
    tdb = Tdb()
    while True:
        try:
            _tdb.reset_instruction_count()
            tdb._runscript(mainpyfile)
            if tdb._user_requested_quit:
                break
            print "The program finished and will be restarted"
        except ReExecute:
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
            tdb.interaction(None, t)
            print "Post mortem debugger finished. The " + mainpyfile + \
                  " will be restarted"


if __name__ == '__main__':
    import tdb
    tdb.main()