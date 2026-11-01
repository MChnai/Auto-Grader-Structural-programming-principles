package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import vn.edu.dlu.autograder.cli.AutoGraderCommand;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CliRegressionTest {
    @TempDir Path temp;

    @Test void rootHelpReturnsZero() {
        assertEquals(0, new CommandLine(new AutoGraderCommand()).execute("--help"));
    }

    @Test void invalidOptionReturnsUsageError() {
        assertEquals(2, new CommandLine(new AutoGraderCommand()).execute("--definitely-invalid"));
    }

    @Test void missingRequiredGradeArgsReturnsUsageError() {
        assertEquals(2, new CommandLine(new AutoGraderCommand()).execute("grade"));
    }

    @Test void nonexistentSubmissionReturnsErrorExitCode() throws Exception {
        Path cases = Files.createDirectories(temp.resolve("cases"));
        assertEquals(1, new CommandLine(new AutoGraderCommand()).execute("grade", temp.resolve("missing.zip").toString(), "-t", cases.toString()));
    }
}
