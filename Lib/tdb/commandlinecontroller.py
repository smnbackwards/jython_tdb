import cmd
import sys
from .controller import Controller

class CommandLineController(Controller, cmd.Cmd):

    def __init__(self):
        Controller.__init__(self)
        cmd.Cmd.__init__(self,completekey='tab', stdin=None, stdout=None)

    do_h = cmd.Cmd.do_help

    def get_cmd(self):
        self.cmdloop()

    def displayhook(self, obj):
        """Custom displayhook for the exec in default(), which prevents
        assignment of the _ variable in the builtins.
        """
        # reproduce the behavior of the standard displayhook, not printing None
        if obj is not None:
            print >> self.stdout, repr(obj)

    def preloop(self):
        self.prompt = "(Tdb)<%s>"%self.debugger.get_ic()

    def default(self, line):
        if line[:1] == '!': line = line[1:]
        locals = self.debugger.curframe_locals
        globals = self.debugger.curframe.f_globals
        try:
            code = compile(line + '\n', '<stdin>', 'single')
            save_stdout = sys.stdout
            save_stdin = sys.stdin
            save_displayhook = sys.displayhook
            try:
                sys.stdin = self.stdin
                sys.stdout = self.stdout
                sys.displayhook = self.displayhook
                exec code in globals, locals
            finally:
                sys.stdout = save_stdout
                sys.stdin = save_stdin
                sys.displayhook = save_displayhook
        except:
            t, v = sys.exc_info()[:2]
            if type(t) == type(''):
                exc_type_name = t
            else: exc_type_name = t.__name__
            print >> self.stdout, '***', exc_type_name + ':', v

    def precmd(self, line):
        """Handle ';;' separator."""
        if not line.strip():
            return line
        args = line.split()
                # split into ';;' separated commands
        # unless it's an alias command
        marker = line.find(';;')
        if marker >= 0:
            # queue up everything after marker
            next = line[marker+2:].lstrip()
            self.cmdqueue.append(next)
            line = line[:marker].rstrip()
        return line

    def onecmd(self, line):
        """Interpret the argument as though it had been typed in response
        to the prompt.

        Checks whether this line is typed at the normal prompt or in
        a breakpoint command list definition.
        """
        cmd, arg, line = self.parseline(line)
        if not line:
            return self.emptyline()
        if cmd is None:
            return self.default(line)
        self.lastcmd = line
        if line == 'EOF' :
            self.lastcmd = ''
        if cmd == '':
            return self.default(line)
        else:
            try:
                func = getattr(self.debugger, 'do_' + cmd)
            except AttributeError:
                return self.default(line)
            return func(arg)

    def get_names(self):
        return dir(self.debugger.__class__)