package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Run grader via child JVM: SubmissionGrader currently keeps an executor pool alive. */
class GraderCliRegressionTest {
    @TempDir Path temp;

    private RegressionFixtures.RunResult grade(String source, String expected, String actualInput) throws Exception {
        RegressionFixtures.requireGxx();
        Path zip = RegressionFixtures.zipOne(temp, "main.cpp", source);
        Path cases = Files.createDirectories(temp.resolve("cases"));
        Files.writeString(cases.resolve("01.in"), actualInput);
        Files.writeString(cases.resolve("01.out"), expected);
        return RegressionFixtures.runCli(temp, "grade", zip.toAbsolutePath().toString(), "--testcases", cases.toAbsolutePath().toString());
    }

    @Test void acceptedSubmissionIsReported() throws Exception {
        var result = grade("#include <iostream>\nint main(){int a,b;std::cin>>a>>b;std::cout<<a+b;}", "30", "10 20\n");
        assertEquals(0, result.exitCode(), result.combinedOutput());
        assertTrue(result.combinedOutput().contains("ACCEPTED"), result.combinedOutput());
    }

    @Test void wrongAnswerIsReported() throws Exception {
        var result = grade("#include <iostream>\nint main(){std::cout<<25;}", "30", "");
        assertEquals(0, result.exitCode(), result.combinedOutput());
        assertTrue(result.combinedOutput().contains("WRONG_ANSWER"), result.combinedOutput());
    }

    @Test void compileFailureIsReported() throws Exception {
        var result = grade("int main( {", "30", "");
        assertEquals(0, result.exitCode(), result.combinedOutput());
        assertTrue(result.combinedOutput().contains("Compile Error"), result.combinedOutput());
    }

    @Test void runtimeFailureIsReported() throws Exception {
        // Deterministic nonzero exit on both Windows and Linux; avoids relying on division-by-zero UB.
        var result = grade("int main(){return 7;}", "30", "");
        assertEquals(0, result.exitCode(), result.combinedOutput());
        assertTrue(result.combinedOutput().contains("RUNTIME_ERROR"), result.combinedOutput());
    }

    @Test void timeoutFailureIsReported() throws Exception {
        var result = grade("int main(){volatile int x=0;while(true){x++;}}", "30", "");
        assertEquals(0, result.exitCode(), result.combinedOutput());
        assertTrue(result.combinedOutput().contains("TIME_LIMIT_EXCEEDED"), result.combinedOutput());
    }
}
