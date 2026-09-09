package vn.edu.dlu.autograder.model;

public class TestCaseResult {

    public enum Status {
        ACCEPTED,               // Đúng đáp án (AC)
        WRONG_ANSWER,           // Sai đáp án (WA)
        TIME_LIMIT_EXCEEDED,    // Quá thời gian (TLE)
        RUNTIME_ERROR,          // Lỗi khi chạy (RTE)
        COMPILE_ERROR           // Lỗi biên dịch (CE)
    }

    private final TestCase testCase;
    private final Status status;
    private final String actualOutput;
    private final String diffMessage;
    private final long executionTimeMs;
    private boolean hasMemoryLeak;

    public TestCaseResult(TestCase testCase, Status status, String actualOutput, String diffMessage, long executionTimeMs) {
        this(testCase, status, actualOutput, diffMessage, executionTimeMs, false);
    }

    public TestCaseResult(TestCase testCase, Status status, String actualOutput, String diffMessage, long executionTimeMs, boolean hasMemoryLeak) {
        this.testCase = testCase;
        this.status = status;
        this.actualOutput = actualOutput;
        this.diffMessage = diffMessage;
        this.executionTimeMs = executionTimeMs;
        this.hasMemoryLeak = hasMemoryLeak;
    }

    public TestCase getTestCase() { return testCase; }
    public Status getStatus() { return status; }
    public String getActualOutput() { return actualOutput; }
    public String getDiffMessage() { return diffMessage; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public boolean isPassed() { return status == Status.ACCEPTED; }

    public boolean hasMemoryLeak() { return hasMemoryLeak; }
    public void setHasMemoryLeak(boolean hasMemoryLeak) { this.hasMemoryLeak = hasMemoryLeak; }
}