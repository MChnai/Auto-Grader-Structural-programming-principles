package vn.edu.dlu.autograder.model;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {
    private boolean valid;
    private List<String> violations;
    private List<String> warnings; // Thêm cảnh báo Clean Code
    private int loopCount;
    private int functionCount;

    // Các thuộc tính bổ sung cho phân tích nâng cao
    private double cleanCodeScore = 100.0;
    private boolean recursive = false;
    private boolean structDefined = false;
    private int cyclomaticComplexity = 1;

    public AnalysisResult() {
        this.valid = true;
        this.violations = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addViolation(String message) {
        this.valid = false;
        if (!violations.contains(message)) {
            violations.add(message);
        }
    }

    public void addWarning(String message, double penalty) {
        this.warnings.add(message);
        this.cleanCodeScore = Math.max(0.0, this.cleanCodeScore - penalty);
    }

    public boolean isValid() { return valid; }
    public List<String> getViolations() { return violations; }
    public int getLoopCount() { return loopCount; }
    public void setLoopCount(int loopCount) { this.loopCount = loopCount; }
    public int getFunctionCount() { return functionCount; }
    public void setFunctionCount(int functionCount) { this.functionCount = functionCount; }
    public List<String> getWarnings() { return warnings; }
    public double getCleanCodeScore() { 
        return Math.min(100.0, Math.max(0.0, cleanCodeScore)); 
    }    public boolean isRecursive() { return recursive; }
    public void setRecursive(boolean recursive) { this.recursive = recursive; }
    public boolean isStructDefined() { return structDefined; }
    public void setStructDefined(boolean structDefined) { this.structDefined = structDefined; }
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }
}