package vn.edu.dlu.autograder.dto;

import java.util.List;

public class PlagiarismRequestDTO {
    private List<String> zipFilePaths;
    private double threshold;

    public List<String> getZipFilePaths() { return zipFilePaths; }
    public void setZipFilePaths(List<String> zipFilePaths) { this.zipFilePaths = zipFilePaths; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
}