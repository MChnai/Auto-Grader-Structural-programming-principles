package vn.edu.dlu.autograder.model;

import java.nio.file.Path;

public class StudentSubmission {
    private final String studentId;   // Ví dụ: "2112345"
    private final String studentName; // Ví dụ: "Nguyen Van A"
    private final Path zipFilePath;   // Đường dẫn file .zip bài nộp

    public StudentSubmission(String studentId, String studentName, Path zipFilePath) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.zipFilePath = zipFilePath;
    }

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Path getZipFilePath() { return zipFilePath; }
}