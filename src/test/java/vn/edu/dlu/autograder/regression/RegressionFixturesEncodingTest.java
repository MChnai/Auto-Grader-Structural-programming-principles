package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Protects CLI regression tests when Windows tools emit mixed code pages. */
class RegressionFixturesEncodingTest {
    @TempDir Path temp;

    @Test
    void malformedUtf8InLocalizedDiagnosticsDoesNotHideAsciiStatus() throws Exception {
        Path log = temp.resolve("cli.log");
        byte[] status = "ACCEPTED\n".getBytes(StandardCharsets.US_ASCII);
        byte[] bytes = java.util.Arrays.copyOf(status, status.length + 2);
        bytes[status.length] = (byte) 0xE1; // Incomplete multibyte sequence.
        bytes[status.length + 1] = (byte) 0xFF; // Not valid in UTF-8.
        Files.write(log, bytes);

        String output = RegressionFixtures.readCliOutput(log);
        assertTrue(output.contains("ACCEPTED"), output);
        assertTrue(output.contains("\uFFFD"), "Malformed diagnostic bytes must be replaced");
    }
}
