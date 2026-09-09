package vn.edu.dlu.autograder.model;

public class GradingRubric {
    private final double maxScore;             // Điểm tối đa (mặc định 10.0)
    private final double testCaseWeightRatio;  // Trọng số cho Testcases (ví dụ 0.8 = 80%)
    private final double codeStyleWeightRatio; // Trọng số cho Clean Code/Style (ví dụ 0.2 = 20%)
    private final double benchmarkTimeMs;      // Thời gian thực thi tiêu chuẩn bài mẫu (ms)

    public GradingRubric(double maxScore, double testCaseWeightRatio, double codeStyleWeightRatio, double benchmarkTimeMs) {
        if (Math.abs((testCaseWeightRatio + codeStyleWeightRatio) - 1.0) > 0.001) {
            throw new IllegalArgumentException("Tổng các trọng số phải bằng 1.0 (100%)!");
        }
        this.maxScore = maxScore;
        this.testCaseWeightRatio = testCaseWeightRatio;
        this.codeStyleWeightRatio = codeStyleWeightRatio;
        this.benchmarkTimeMs = benchmarkTimeMs;
    }

    public GradingRubric(double maxScore, double testCaseWeightRatio, double codeStyleWeightRatio) {
        this(maxScore, testCaseWeightRatio, codeStyleWeightRatio, 200.0); // Mặc định 200ms
    }

    // Constructor mặc định: Thang điểm 10, 100% dựa vào Testcases, Benchmark 200ms
    public GradingRubric() {
        this(10.0, 1.0, 0.0, 200.0);
    }

    public double getMaxScore() { return maxScore; }
    public double getTestCaseWeightRatio() { return testCaseWeightRatio; }
    public double getCodeStyleWeightRatio() { return codeStyleWeightRatio; }
    public double getBenchmarkTimeMs() { return benchmarkTimeMs; }
}