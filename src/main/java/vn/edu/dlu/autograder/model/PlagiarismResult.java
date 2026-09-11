package vn.edu.dlu.autograder.model;

public class PlagiarismResult {
    private final String studentA;
    private final String studentB;
    private final double similarityPercentage; // % Tương đồng (0 - 100%)

    public PlagiarismResult(String studentA, String studentB, double similarityPercentage) {
        this.studentA = studentA;
        this.studentB = studentB;
        this.similarityPercentage = similarityPercentage;
    }

    public String getStudentA() { return studentA; }
    public String getStudentB() { return studentB; }
    public double getSimilarityPercentage() { return similarityPercentage; }
}