package org.python.modules._odb;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nms12 on 5/18/2016.
 */
public class BreakpointManager {

    private int index;
    private final Set<Breakpoint> breakpoints;

    public BreakpointManager(){
        index = 0;
        breakpoints = new LinkedHashSet<>();
    }

    public boolean checkIndex(int index){
        return index >= 0 && index < this.index;
    }

    public List<Integer> getBreakpointLinesForFile(String filename){
        return breakpoints.stream()
                .filter(b -> filename.equals(b.getFilename()))
                .map(b -> b.getLineno())
                .collect(Collectors.toList());
    }

    public boolean hasBreakpoints(){
        return !breakpoints.isEmpty();
    }

    public Breakpoint insert(String filename, int lineno){
        Breakpoint breakpoint = new Breakpoint(filename, lineno, index);
        breakpoints.add(breakpoint);
        index++;
        return breakpoint;
    }

    public Breakpoint getBreakpoint(String filename, int lineno){
        Optional<Breakpoint> breakpoint = breakpoints.stream().filter(b -> b.equals(filename, lineno)).findFirst();
        return breakpoint.isPresent() ? breakpoint.get() : null;
    }

    public Collection<Breakpoint> getBreakpoints(){
        return breakpoints;
    }

    public void clearAll() {
        breakpoints.clear();
    }

    public boolean clear(String filename, int lineno) {
        return breakpoints.remove(new Breakpoint(filename, lineno, -1));
    }

    public boolean clear(int index) {
        for(Iterator<Breakpoint> iterator = breakpoints.iterator(); iterator.hasNext(); ){
            Breakpoint bp = iterator.next();
            if(bp.index == index){
                iterator.remove();
                return true;
            }
        }

        return false;
    }

}
