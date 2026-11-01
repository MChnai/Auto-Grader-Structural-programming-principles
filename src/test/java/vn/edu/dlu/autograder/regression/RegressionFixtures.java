package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Assumptions;
import vn.edu.dlu.autograder.executor.CompilerEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Benign, temporary fixtures. Never run arbitrary student code in these baseline tests. */
final class RegressionFixtures {
    private RegressionFixtures() {}

    static void zip(Path output, Map<String, String> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

    static Path zipOne(Path dir, String entry, String code) throws IOException {
        Path zip = dir.resolve("submission.zip");
        Map<String, String> files = new LinkedHashMap<>();
        files.put(entry, code);
        zip(zip, files);
        return zip;
    }

    static void requireGxx() {
        try {
            Process probe = new ProcessBuilder("g++", "--version").redirectErrorStream(true).start();
            if (!probe.waitFor(5, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                Assumptions.assumeTrue(false, "g++ version check timed out");
            }
            Assumptions.assumeTrue(probe.exitValue() == 0, "g++ is not available");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            Assumptions.assumeTrue(false, "g++ not available: " + e.getMessage());
        }
    }

    static Path compile(Path root, Map<String, String> files) throws IOException {
        requireGxx();
        Path source = Files.createDirectories(root.resolve("cpp"));
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path file = source.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }
        CompilerEngine.CompileResult result = assertTimeoutPreemptively(Duration.ofSeconds(18),
                () -> new CompilerEngine().compile(source, root.resolve("bin")));
        assertTrue(result.isSuccess(), () -> "Compilation failed: " + result.getCompilerMessage());
        assertNotNull(result.getExecutablePath());
        return result.getExecutablePath();
    }

    static Map<String, String> oneCpp(String code) {
        return Map.of("main.cpp", code);
    }

    static RunResult runCli(Path workdir, String... args) throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java").toString();
        // Surefire may use a manifest-only booter JAR as java.class.path.
        String testClasspath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        String cp = java.util.Arrays.stream(testClasspath.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator)))
                .map(entry -> Path.of(entry).toAbsolutePath().toString())
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        java.util.List<String> command = new java.util.ArrayList<>();
        command.add(javaExecutable);
        command.add("-cp");
        command.add(cp);
        command.add("vn.edu.dlu.autograder.cli.AutoGraderCommand");
        java.util.Collections.addAll(command, args);
        ProcessBuilder builder = new ProcessBuilder(command).directory(workdir.toFile());
        Path log = workdir.resolve("cli-output-" + System.nanoTime() + ".log");
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        Process process = builder.start();
        try {
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                process.waitFor(3, TimeUnit.SECONDS);
                fail("CLI exceeded 20-second watchdog");
            }
            return new RunResult(process.exitValue(), readCliOutput(log));
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    /**
     * The child JVM and native compiler may emit different Windows code pages into
     * the same redirected log. Files.readString(..., UTF_8) rejects malformed
     * sequences; for CLI regression tests we only assert ASCII markers/statuses.
     * Retain raw bytes on disk and decode lossily so localized messages cannot
     * make an otherwise completed grader test crash while parsing the log.
     */
    static String readCliOutput(Path log) throws IOException {
        return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
    }

    record RunResult(int exitCode, String combinedOutput) {}
}
