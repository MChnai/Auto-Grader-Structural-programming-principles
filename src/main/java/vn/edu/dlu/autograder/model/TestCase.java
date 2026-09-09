package vn.edu.dlu.autograder.model;

public class TestCase {
    private String id;
    private String input;
    private String expectedOutput;
    private int timeLimitSeconds;

    public TestCase(String id, String input, String expectedOutput, int timeLimitSeconds) {
        this.id = id;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public String getId() { return id; }
    public String getInput() { return input; }
    public String getExpectedOutput() { return expectedOutput; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
}