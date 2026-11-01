package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.analyzer.CppCodeAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzerRegressionTest {
    @TempDir Path temp;

    @Test void countsSimpleFunctionAndLoop() throws Exception {
        Path source = temp.resolve("main.cpp");
        Files.writeString(source, "int helper(){return 1;} int main(){for(int i=0;i<3;i++){} return helper();}");
        var result = new CppCodeAnalyzer().analyze(source);
        assertTrue(result.isValid(), () -> result.getViolations().toString());
        assertEquals(2, result.getFunctionCount());
        assertEquals(1, result.getLoopCount());
    }

    @Test void ignoresKeywordsInsideCommentsAndStrings() throws Exception {
        Path source = temp.resolve("main.cpp");
        Files.writeString(source, "// for while\n/* for() */ int main(){const char* s=\"while for\";return 0;}");
        var result = new CppCodeAnalyzer().analyze(source);
        assertEquals(0, result.getLoopCount());
        assertEquals(1, result.getFunctionCount());
    }

    @Test void reportsMissingFile() {
        var result = new CppCodeAnalyzer().analyze(temp.resolve("not-present.cpp"));
        assertFalse(result.isValid());
        assertFalse(result.getViolations().isEmpty());
    }
}
