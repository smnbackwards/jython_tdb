package org.python.core;

import java.util.Stack;

/**
 * Created by dual- on 2/18/2016.
 */
public class TdbTraceFunction extends PythonTraceFunction {
    private static long instructionCount = 0;
    private static long callDepth = 0;
    public static Stack<Long> callReturnMap = new Stack<>();
    public static long lastCallInstructionCount = 0;
    public static String file;
    public static boolean waitForMainPyFile = true;

    public static int stopIc = 0;
    public static int stopDepth = -1;
    public static String stopEvent = null;
    public static boolean redoMode = false;
    public static String event = null;
    public static String lastEvent = null;

    TdbTraceFunction(PyObject tracefunc) {
        super(tracefunc);
    }

    public static void reset(){
        instructionCount = 0;
        callDepth = 0;
        callReturnMap = new Stack<>();
        lastCallInstructionCount = 0;
        waitForMainPyFile = true;

        stopIc = 0;
        stopDepth = -1;
        stopEvent = null;
        redoMode = false;
        event = null;
        lastEvent = null;
    }

    public static void resetInstructionCount() {
        instructionCount = 0;
        callDepth = 0;
        callReturnMap = new Stack<>();
        lastCallInstructionCount = 0;
        waitForMainPyFile = true;
    }

    public static long getInstructionCount() {
        return instructionCount;
    }

    public static long getCallDepth() {
        return callDepth;
    }

    public static long getLastCallInstructionCount() {
        return lastCallInstructionCount;
    }

    public static long getPreviousCallInstructionCount(){
        return callReturnMap.size() > 0 ? callReturnMap.peek() : 0;
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
                    if (frame.f_code.co_filename.startsWith(Py.getSystemState().path.get(1).toString())) {
                        return this;
                    }

                    if (waitForMainPyFile) {
                        if (ts.frame.f_code.co_filename.equals("<string>")) {
                            return this;
                        }

                        if (label.equals("call")) {
                            waitForMainPyFile = false;
                            return this;
                        }
                    }


                    if (label.equals("call")) {
                        callDepth++;
                        lastCallInstructionCount = instructionCount;
                        callReturnMap.push(lastCallInstructionCount);
                    }

                    event = label;
                    if (stopIc >= 0 && instructionCount >= stopIc) {
                        if (stopDepth == -1 || stopDepth >= callDepth) {
                            if (stopEvent == null || label.equals(stopEvent)) {
                                redoMode = false;
                                ret = tracefunc.__call__(frame, new PyString(label), arg);
                            }
                        }
                    }
                    ret = tracefunc;
                    lastEvent = label;

                    instructionCount++;
                    if (label.equals("return")) {
                        if (callDepth == 0) {
                            tracefunc = null;
                            ts.tracefunc = null;
                            return null;
                        }

                        lastCallInstructionCount = callReturnMap.pop();
                        callDepth--;
                    }

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
