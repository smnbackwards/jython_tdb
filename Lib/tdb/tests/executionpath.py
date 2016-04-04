import linecache
import sys
import _tdb

events = []


def tracefunc(frame, event, arg):
    filename = frame.f_code.co_filename
    linenumber = frame.f_lineno
    line = linecache.getline(filename, linenumber)
    events.append((_tdb.instruction_count(), _tdb.call_depth(), event, line, linenumber))
    return tracefunc


def print_graph(events):
    maxdepth = 0
    for e in events:
        maxdepth = max(e[1], maxdepth)
    lines = [['  ' for i in range(len(events))] for j in range(maxdepth+1)]
    for i in range(maxdepth+1):
        line = [e for e in events if e[1] == i]
        for e in line:
            #e[0]-1 because the instruction count is offset by 1
            lines[i][e[0]-1] = str(e[4]).ljust(2)


    #Terrible -3 hack to remove extra instruction counts caused by this program
    print 'ic: ' + '  '.join([str(i).ljust(2) for i in range(len(events)-1)])
    print '-'*(len(events)*4)
    print '\n'.join([str(i).ljust(3)+'|'+'  '.join(l[1:]) for i,l in enumerate(lines)])

    # for ic,depth,e,ln,lno in events:


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print __doc__
        sys.exit()
    else:
        file_name = sys.argv[1]

    sys.settrace(tracefunc)
    globals = {}
    locals = {}
    try:
        execfile(file_name)
    finally:
        sys.settrace(None)
    print_graph(events)
