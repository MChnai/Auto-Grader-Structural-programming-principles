package vn.edu.dlu.autograder.model;

import java.util.ArrayList;
import java.util.List;

public class AnalysisResult {
    private boolean valid;
    private List<String> violations;
    private List<String> warnings; 
    private int loopCount;
    private int functionCount;
    private double cleanCodeScore = 100.0;
    private boolean recursive = false;
    private boolean structDefined = false;
    private int cyclomaticComplexity = 1;
    private int namespaceCount = 0;
    private List<String> detectedPackages = new ArrayList<>();

    public int getNamespaceCount() { return namespaceCount; }
    public void setNamespaceCount(int namespaceCount) { this.namespaceCount = namespaceCount; }

    public List<String> getDetectedPackages() { return detectedPackages; }
    public void addDetectedPackage(String packageName) { this.detectedPackages.add(packageName); }

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
    public void evaluateArchitectureScore() {
        if (namespaceCount > 0 || !detectedPackages.isEmpty()) {
            this.warnings.add("Cộng điểm kiến trúc: Bài nộp có tổ chức Package/Namespace rõ ràng (+5 điểm Clean Code).");
            this.cleanCodeScore = Math.min(100.0, this.cleanCodeScore + 5.0);
        }

        if (functionCount > 5 && namespaceCount == 0 && detectedPackages.isEmpty()) {
            this.warnings.add("Cảnh báo kiến trúc: Bài nộp có trên 5 hàm nhưng không chia Namespace/Package (-5 điểm Clean Code).");
            this.cleanCodeScore = Math.max(0.0, this.cleanCodeScore - 5.0);
        }
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