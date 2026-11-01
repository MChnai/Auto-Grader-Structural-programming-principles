package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.executor.NativeProcessExecutor;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class NativeExecutorRegressionTest {
    @TempDir Path temp;

    @Test void reportsExitCodeAndStdout() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "#include <iostream>\nint main(){std::cout<<30<<'\\n';return 0;}"));
        var result = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> NativeProcessExecutor.runWithTimeout(exe, "", 2));
        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals("30", result.getStdout().strip()); // works on CRLF and LF
    }

    @Test void reportsTimeoutOnInfiniteLoop() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "int main(){volatile int x=0;while(true){x++;}}"));
        var result = assertTimeoutPreemptively(Duration.ofSeconds(6),
                () -> NativeProcessExecutor.runWithTimeout(exe, "", 1));
        assertTrue(result.isTimedOut());
    }

    @Test void deliversLargeInputToProgramWhichReadsIt() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp(
                "#include <iostream>\nint main(){char c;unsigned long long n=0;" +
                "while(std::cin.get(c)){++n;}std::cout<<n;}"));
        String input = "a".repeat(2_000_000);
        var result = assertTimeoutPreemptively(Duration.ofSeconds(8),
                () -> NativeProcessExecutor.runWithTimeout(exe, input, 4));
        assertFalse(result.isTimedOut(), result.getStderr());
        assertEquals(0, result.getExitCode(), result.getStderr());
        assertEquals("2000000", result.getStdout().strip());
    }
}
