package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.executor.CompilerEngine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CompilerRegressionTest {
    @TempDir Path temp;

    @Test void compilesSingleFile() throws Exception {
        Path exe = RegressionFixtures.compile(temp, RegressionFixtures.oneCpp("int main(){return 0;}"));
        assertTrue(Files.isRegularFile(exe));
    }

    @Test void compilesMultipleCppFiles() throws Exception {
        Path exe = RegressionFixtures.compile(temp, java.util.Map.of(
                "main.cpp", "int twice(int); int main(){return twice(3)==6?0:1;}",
                "lib/twice.cpp", "int twice(int n){return n*2;}"));
        assertTrue(Files.isRegularFile(exe));
    }

    @Test void invalidCppReturnsCompilerError() throws Exception {
        RegressionFixtures.requireGxx();
        Path src = Files.createDirectories(temp.resolve("src"));
        Files.writeString(src.resolve("main.cpp"), "int main( { ");
        var result = new CompilerEngine().compile(src, temp.resolve("bin"));
        assertFalse(result.isSuccess());
        assertNull(result.getExecutablePath());
    }

    @Test void missingSourceDirectoryIsRejected() {
        var result = new CompilerEngine().compile(temp.resolve("not-present"), temp.resolve("bin"));
        assertFalse(result.isSuccess());
    }
}
