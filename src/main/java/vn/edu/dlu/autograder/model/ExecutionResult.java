package vn.edu.dlu.autograder.model;

public class ExecutionResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long executionTimeMs;
    private final boolean isTimedOut;
    private final boolean hasMemoryLeak;

    public ExecutionResult(int exitCode, String stdout, String stderr, long executionTimeMs, boolean isTimedOut, boolean hasMemoryLeak) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.executionTimeMs = executionTimeMs;
        this.isTimedOut = isTimedOut;
        this.hasMemoryLeak = hasMemoryLeak;
    }

    public ExecutionResult(int exitCode, String stdout, String stderr, long executionTimeMs, boolean isTimedOut) {
        this(exitCode, stdout, stderr, executionTimeMs, isTimedOut, false);
    }

    public int getExitCode() { return exitCode; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public boolean isTimedOut() { return isTimedOut; }
    public boolean hasMemoryLeak() { return hasMemoryLeak; }
}