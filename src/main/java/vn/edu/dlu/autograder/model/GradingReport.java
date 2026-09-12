package vn.edu.dlu.autograder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GradingReport {
    private final StudentSubmission submission;
    private final boolean compileSuccess;
    private final String compileMessage;
    private final List<TestCaseResult> testCaseResults;
    private final double finalScore;
    private final String summary;
    private double cleanCodeScore = 100.0;
    private List<String> styleWarnings = new ArrayList<>();
    private long maxExecutionTimeMs = 0;
    private long maxMemoryUsedKb = 0;
    private double plagiarismSimilarity = 0.0; 

    public GradingReport(StudentSubmission submission, 
                         boolean compileSuccess, 
                         String compileMessage, 
                         List<TestCaseResult> testCaseResults, 
                         double finalScore, 
                         String summary) {
        this.submission = submission;
        this.compileSuccess = compileSuccess;
        this.compileMessage = compileMessage;
        this.testCaseResults = testCaseResults != null ? new ArrayList<>(testCaseResults) : Collections.emptyList();
        this.finalScore = finalScore;
        this.summary = summary;
        this.calculateMetrics();
    }

    private void calculateMetrics() {
        if (testCaseResults != null) {
            for (TestCaseResult tr : testCaseResults) {
                if (tr.getExecutionTimeMs() > this.maxExecutionTimeMs) {
                    this.maxExecutionTimeMs = tr.getExecutionTimeMs();
                }
            }
        }
    }

    public static GradingReport compileError(StudentSubmission submission, String compileMessage) {
        return new GradingReport(submission, false, compileMessage, Collections.emptyList(), 0.0, "Compile Error (Lỗi biên dịch)");
    }

    public static GradingReport extractionError(StudentSubmission submission, String errorMessage) {
        return new GradingReport(submission, false, errorMessage, Collections.emptyList(), 0.0, "Extraction Error: " + errorMessage);
    }

    public StudentSubmission getSubmission() { return submission; }
    public boolean isCompileSuccess() { return compileSuccess; }
    public String getCompileMessage() { return compileMessage; }
    public List<TestCaseResult> getTestCaseResults() { return Collections.unmodifiableList(testCaseResults); }
    public double getFinalScore() { return finalScore; }
    public String getSummary() { return summary; }

    public int getPassedTestCount() {
        return (int) testCaseResults.stream().filter(TestCaseResult::isPassed).count();
    }

    public int getTotalTestCount() { return testCaseResults.size(); }

    public double getCleanCodeScore() { return cleanCodeScore; }
    public void setCleanCodeScore(double cleanCodeScore) { this.cleanCodeScore = cleanCodeScore; }
    
    public List<String> getStyleWarnings() { return Collections.unmodifiableList(styleWarnings); }
    public void setStyleWarnings(List<String> styleWarnings) { 
        this.styleWarnings = styleWarnings != null ? new ArrayList<>(styleWarnings) : new ArrayList<>(); 
    }

    public long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
    public double getPlagiarismSimilarity() { return plagiarismSimilarity; }
    public void setPlagiarismSimilarity(double plagiarismSimilarity) { this.plagiarismSimilarity = plagiarismSimilarity; }
}