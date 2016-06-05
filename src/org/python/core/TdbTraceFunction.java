package org.python.core;

import java.util.Stack;

/**
 * Created by dual- on 2/18/2016.
 */
public class TdbTraceFunction extends PythonTraceFunction {
    private static long instructionCount = 0;
    private static long callDepth = 0;
    public static boolean isTracing = false;
    public static Stack<Long> callReturnMap = new Stack<>();
    public static long lastCallInstructionCount = 0;
    public static String file;
    public static boolean waitForMainPyFile = true;

    TdbTraceFunction(PyObject tracefunc) {
        super(tracefunc);
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

    public static long getReturnInstruction() {
        return lastCallInstructionCount;
    }

    public static long getLastCallInstructionCountAtCurrentLevel() {
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

                    if (callDepth == 0 && label.equals("return")) {
                        tracefunc = null;
                        return null;
                    }

                    if (label.equals("call")) {
                        callDepth++;
                        lastCallInstructionCount = instructionCount;
                        callReturnMap.push(lastCallInstructionCount);
                    }
                    if (label.equals("return")) {
                        lastCallInstructionCount = callReturnMap.pop();
                        callDepth--;
                    }

                    isTracing = true;
                    ret = tracefunc.__call__(frame, new PyString(label), arg);
                    instructionCount++;
                    isTracing = false;
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
