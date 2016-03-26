package org.python.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dual- on 2/18/2016.
 */
public class TdbTraceFunction extends PythonTraceFunction {
    private static long instructionCount = 0;
    private static long callDepth = 0;
    public static boolean isTracing = false;
    public static Map<Long, Long> callReturnMap = new HashMap<>();
    public static Long lastCallInstructionCount = -1L;
    public static String file;

    TdbTraceFunction(PyObject tracefunc) {
        super(tracefunc);
    }

    public void incInstructionCount(PyFrame frame) {
        instructionCount += 1;
//        System.out.println(instructionCount);
    }

    public static void resetInstructionCount() {
        instructionCount = 0;
        callDepth = 0;
    }

    public static long getInstructionCount() {
        return instructionCount;
    }

    public static long getCallDepth(){
        return callDepth;
    }

    public static long getReturnInstruction(long depth){
        assert callReturnMap.containsKey(depth);

        return callReturnMap.get(depth);
    }

    public static long getLastCallInstructionCountAtCurrentLevel(){
        return lastCallInstructionCount;
    }

    @Override
    protected TraceFunction safeCall(PyFrame frame, String label, PyObject arg) {
        synchronized (imp.class) {
            synchronized (this) {
                ThreadState ts = Py.getThreadState();
                if (ts.tracing)
                    return null;
                if (tracefunc == null)
                    return null;
                PyObject ret = null;
                try {
                    ts.tracing = true;
                    if (frame.f_code.co_filename.endsWith("bdb.py")) {
                        return this;
                    }

                    incInstructionCount(frame);

                    if (label.equals("call")) {
                        callDepth++;
                        callReturnMap.put(callDepth, instructionCount-1);
                    }
                    if (label.equals("return")) {
                        lastCallInstructionCount =  callReturnMap.remove(callDepth);
                        callDepth--;
                    }


                    //System.out.println(getInstructionCount()+" Trace calling"+label);
                    isTracing = true;
                    //filename = frame.pycode.co_filename
                    int linenumber = frame.f_lineno;
                    System.out.println(frame.f_code.co_filename + " " + frame.f_lineno + " " + getInstructionCount() + " @ " + callDepth);
                    ret = tracefunc.__call__(frame, new PyString(label), arg);
                    isTracing = false;
                    //System.out.println(getInstructionCount()+" Trace returned"+label);
                } catch (PyException exc) {
                    frame.tracefunc = null;
                    ts.tracefunc = null;
                    ts.profilefunc = null;
                    throw exc;
                } finally {
                    ts.tracing = false;
                }
                if (ret == tracefunc)
                    return this;
                if (ret == Py.None)
                    return null;
                return new TdbTraceFunction(ret);
            }
        }
    }

    @Override
    public TraceFunction traceCall(PyFrame frame) {
        return safeCall(frame, "call", Py.None);
    }

    @Override
    public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
        return safeCall(frame, "return", ret);
    }

    @Override
    public TraceFunction traceLine(PyFrame frame, int line) {
        return safeCall(frame, "line", Py.None);
    }

    @Override
    public TraceFunction traceException(PyFrame frame, PyException exc) {
        // We must avoid passing a null to a PyTuple
        PyObject safeTraceback = exc.traceback == null ? Py.None : exc.traceback;
        return safeCall(frame, "exception",
                new PyTuple(exc.type, exc.value, safeTraceback));
    }
}
