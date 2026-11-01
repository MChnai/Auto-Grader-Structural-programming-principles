package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.analyzer.CppCodeAnalyzer;
import vn.edu.dlu.autograder.archive.ZipExtractor;
import vn.edu.dlu.autograder.checker.OutputChecker;
import vn.edu.dlu.autograder.executor.NativeProcessExecutor;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Desired post-fix behavior for known P00.2 defects.
 * Disabled by default to keep the P00 baseline green; only enable after fixing each defect.
 * See docs/baseline/P00.3-known-defects.csv for the full 14-case register.
 */
class KnownDefectsTest {
    @TempDir Path temp;

    @Disabled("B-04 / P02: archive decompression quota not implemented")
    @Test void shouldRejectArchiveExceedingConfiguredDecompressionQuota() throws Exception {
        Path archive = RegressionFixtures.zipOne(temp, "main.cpp", "x".repeat(6_000_000));
        var result = ZipExtractor.extract(archive, temp.resolve("unpacked"));
        assertFalse(result.isSuccess(), "Requires a documented and configurable decompression quota");
    }

    @Disabled("B-01 / P02: native executor waits for child before draining stdout")
    @Test void shouldDrainLargeStdoutWithoutTimeout() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "#include <iostream>\nint main(){for(int i=0;i<100000;i++)std::cout<<'x';}"));
        var result = NativeProcessExecutor.runWithTimeout(exe, "", 2);
        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals(100000, result.getStdout().length());
    }

    @Disabled("B-01 / P02: native executor waits for child before draining stderr")
    @Test void shouldDrainLargeStderrWithoutTimeout() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "#include <iostream>\nint main(){for(int i=0;i<100000;i++)std::cerr<<'x';}"));
        var result = NativeProcessExecutor.runWithTimeout(exe, "", 2);
        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals(100000, result.getStderr().length());
    }

    @Disabled("B-01 / P02: empty stdin must be closed for programs reading to EOF")
    @Test void shouldCloseEmptyStdin() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "#include <iostream>\nint main(){char c;while(std::cin.get(c)){} std::cout<<1;}"));
        var result = NativeProcessExecutor.runWithTimeout(exe, "", 2);
        assertFalse(result.isTimedOut());
        assertEquals("1", result.getStdout().strip());
    }

    @Disabled("B-06 / P04: Regex analyzer does not handle trailing-return declarations")
    @Test void shouldRecognizeTrailingReturnType() throws Exception {
        Path source = temp.resolve("main.cpp");
        Files.writeString(source, "auto helper() -> int {return 1;} int main(){return helper();}");
        var result = new CppCodeAnalyzer().analyze(source);
        assertEquals(2, result.getFunctionCount());
    }

    @Disabled("B-07 / P03/P10: floating-point tolerance is not implemented")
    @Test void shouldCompareFloatingPointNumbersWithTolerance() {
        // New mode is intentionally resolved dynamically: it does not exist in the V2 baseline.
        // After implementing the mode, enable the test and set a documented epsilon policy.
        var numericMode = Enum.valueOf(OutputChecker.MatchMode.class, "FLOAT_TOLERANCE");
        var result = new OutputChecker().check("0.3000001", "0.3", numericMode);
        assertTrue(result.isMatched());
    }

    @Disabled("B-07 / P03/P10: standalone CR is currently removed instead of treated as line break")
    @Test void shouldNormalizeStandaloneCarriageReturns() {
        var result = new OutputChecker().check("first\rsecond", "first\nsecond", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);
        assertTrue(result.isMatched());
    }
}
