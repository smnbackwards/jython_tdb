'''
usage: python executionpath.py file.py
'''
import linecache
import sys
import time

import matplotlib.pyplot as plt
import matplotlib.path as mpath
import matplotlib.patches as mpatches

import numpy as np

events = []

depth = 0
ic = -1
l = None
h = None


def tracefunc(frame, event, arg):
    global ic, depth
    filename = frame.f_code.co_filename
    linenumber = frame.f_lineno
    line = linecache.getline(filename, linenumber)

    if event == 'call':
        depth = depth + 1

    events.append((ic, depth, event, line, linenumber))

    if event == 'return':
        depth = depth - 1
    ic += 1
    return tracefunc


def plot_graph(events, show_lineno=False, show_ic=False):
    depths = np.array([depth for ic,depth,event,line,linenumber in events])
    ics = np.array([ic for ic,depth,event,line,linenumber in events])

    calls = np.array([[ic,depth] for ic,depth,event,line,linenumber in events if event == 'call'])
    returns = np.array([[ic,depth] for ic,depth,event,line,linenumber in events if event == 'return'])

    # Plot settings
    plt.rcParams.update({'font.size': 18})
    ax = plt.gca()
    plt.xticks(np.arange(0, max(ics)+1, 5))
    plt.yticks(np.arange(0, max(depths)+1, 1.0))
    plt.tick_params(axis='both', which='major', labelsize=14)

    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.spines['left'].set_visible(False)

    ax.yaxis.grid()
    for tic in ax.yaxis.get_major_ticks():
        tic.tick1On = tic.tick2On = False
    for tic in ax.xaxis.get_major_ticks():
        tic.tick1On = tic.tick2On = False
    ax.grid(True)
    ax.set_axisbelow(True)
    plt.ylabel('Call Depth')
    plt.xlabel('Instruction Count')

    if show_lineno :
        plt.axis([-1,max(ics)+2, max(depths)+2, -1])
    else :
        plt.axis([-1,max(ics)+2, max(depths)+1, -1])
    ax.set_autoscale_on(False)

    plt.tight_layout()

    # The line and | ticks
    ax.plot(ics, depths, 'k-|', linewidth=2, markersize=10)
    # The white o call markers
    ax.plot(calls.T[0], calls.T[1], 'wo', markersize=7)
    # The black o return markers
    ax.plot(returns.T[0], returns.T[1], 'ko', markersize=7)

    if show_lineno:
        for ic,depth,event,line,linenumber in events:
            ax.annotate(linenumber, (ic,depth),
                        xytext=(0,-10), textcoords='offset points',
                        horizontalalignment='center', verticalalignment='top')
    if show_ic:
        for ic,depth,event,line,linenumber in events:
            ax.annotate(ic, (ic,depth),
                        xytext=(0,-10), textcoords='offset points',
                        horizontalalignment='center', verticalalignment='top')


def addArrow(x1,y1,x2,y2, reverse=False):
    arrowhead = '-|>' if not reverse else '<|-'
    color = '#1CADE4'
    linewidth = 2.0
    if x1 < x2:
        y1 = y1 - 0.1
        y2 = y2 - 0.1
    else:
        y1 = y1 + 0.1
        y2 = y2 + 0.1
    if y1 != y2 :
        line = mpatches.FancyArrowPatch(posA=(x1, y1), posB=(x2, y2),
                                            arrowstyle='-', ec=color, fc=color, linestyle='dotted', linewidth=linewidth,
                                            connectionstyle="arc3,rad=-0.5",
                                            mutation_scale  =20)
        head = mpatches.FancyArrowPatch(posA=(x1, y1), posB=(x2, y2),
                                        arrowstyle=arrowhead, ec=None, fc=color, linewidth=0.0,
                                        connectionstyle="arc3,rad=-0.5",
                                        mutation_scale=20)
    else :
        cx = float(x1+x2)/2
        cy = y1 - 1 if x1 < x2 else  y1 + 1
        Path = mpath.Path
        path_data = [
            (Path.MOVETO, (x1,y1)),
            (Path.CURVE3, (cx, cy)),
            (Path.CURVE3, (x2, y2)),
            ]
        codes, verts = zip(*path_data)
        path = mpath.Path(verts, codes)
        line = mpatches.FancyArrowPatch( path=path, arrowstyle='-', ec=color, fc=color, linestyle='dotted', linewidth=linewidth, mutation_scale=20)
        head = mpatches.FancyArrowPatch( path=path, arrowstyle=arrowhead, ec=None, fc=color, linewidth=0.0, mutation_scale=20)
    ax = plt.gca()
    ax.add_patch(line)
    ax.add_patch(head)
    return (line,head)

def addMarker(x,y):
    ax = plt.gca()
    return ax.plot([x],[y], color='#1CADE4', marker='o', markersize=16, zorder=0)[0]

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

    events = events[1:]

    plt.ion()
    fig = plt.figure(figsize=(11.5, 6))
    plot_graph(events)
    plt.show()
    plt.draw()

    def update_gui():
        x,y = -1, 0
        marker = None
        arrows = []
        while True:
            time.sleep(0.05)
            i = int(input())

            if i == -1:
                for l,h in arrows:
                    l.remove()
                    h.remove()
                arrows = []
                continue

            ic,depth,event,line,linenumber = events[i]
            if not x == ic or not y == depth :
                arrows.append(addArrow(x,y, ic, depth))
                x = ic
                y = depth
                if marker:
                    marker.remove()
                marker = addMarker(x,y)

    from threading import Thread
    t = Thread(target=update_gui)
    t.start()

    while True:
        plt.pause(0.1)