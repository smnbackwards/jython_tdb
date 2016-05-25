package org.python.modules._odb;

import java.util.Objects;

/**
 * Created by nms12 on 5/18/2016.
 */
public class Breakpoint {

    private final int lineno;
    private final String filename;
    protected int index;

    public Breakpoint(String filename, int lineno, int index){
        this.lineno = lineno;
        this.filename = filename;
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("%-4dbreakpoint   at %s:%d",index, filename, lineno);
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof Breakpoint){
            Breakpoint b = (Breakpoint) obj;
            return Objects.equals(this.filename, b.filename) && this.lineno == b.lineno;
        }

        return false;
    }

    public boolean equals(String filename, int lineno){
        return this.equals(new Breakpoint(filename, lineno, -1));
    }

    public String getFilename(){
        return filename;
    }

    public int getLineno(){
        return lineno;
    }

}
