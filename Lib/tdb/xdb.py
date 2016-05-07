import sys

import traceback

import types

import os
import _odb
import _tdb
from .tpdb import Tpdb, ReExecute, Restart

class Xdb(Tpdb):
    def __init__(self, skip=None):
        Tpdb.__init__(self)
        self.restoremode = False

    def interaction(self, frame, traceback):
        if (not self.redomode) and self.restoremode:
            #we have reached the restore checkpoint
            self.restoremode = False
            _tdb.reset_instruction_count(18,3)
            _odb.do_jump(18)
            odbframe = _odb.getCurrentFrame()
            while frame and odbframe :
                print frame
                print odbframe
                _odb.rebuildFrame(frame, odbframe)
                frame = frame.f_back
                odbframe = odbframe.parent

        return Tpdb.interaction(self, frame, traceback)

    def no_op_trace(self, frame, event, arg):
        return self.no_op_trace

    def dry_run(self, filename):
        import __main__
        __main__.__dict__.clear()
        __main__.__dict__.update({"__name__": "__main__",
                                  "__file__": filename,
                                  "__builtins__": __builtins__,
                                  })

        self.mainpyfile = self.canonic(filename)
        cmd = 'execfile(%r)' % filename

        globals = __main__.__dict__
        locals = globals

        print 'Running %s to create checkpoints'%filename
        _odb.reset()
        sys.settrace(self.no_op_trace)
        if not isinstance(cmd, types.CodeType):
            cmd = cmd + '\n'
        try:
            exec cmd in globals, locals
        finally:
            sys.settrace(None)

        print "Program has finished, now entering ODB mode"
        _odb.setup()

def mainloop(dbg, file):
    dbg.dry_run(file)
    dbg.reset();
    while True:
        try:
            dbg._runscript(file)
            if dbg._user_requested_quit:
                break
            print "The program finished after executing", dbg.get_ic() , "instructions and will be restarted"
        except ReExecute:
            print "Re-storing Checkpoint at IC 18"
            dbg.set_rstep(12) #hard coded value - this is 18 in the new program
            dbg.redomode = True
            dbg.restoremode = True
            dbg._runscript(file[:-3]+'_18.py')
        except Restart:
            print "Restarting", file, "with arguments:"
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
            dbg.interaction(None, t)
            print "Post mortem debugger finished. The " + file + \
                  " will be restarted"
        if dbg._user_requested_quit:
            break

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

    dbg = Xdb()
    mainloop(dbg,mainpyfile)


# When invoked as main program, invoke the debugger on a script
if __name__ == '__main__':
    import tdb.xdb
    tdb.xdb.main()
