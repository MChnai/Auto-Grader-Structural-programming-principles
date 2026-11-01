package vn.edu.dlu.autograder.regression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.archive.ZipExtractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveRegressionTest {
    @TempDir Path temp;

    @Test void extractsNormalCppSubmission() throws Exception {
        Path archive = RegressionFixtures.zipOne(temp, "main.cpp", "int main(){return 0;}");
        var result = ZipExtractor.extract(archive, temp.resolve("output"));
        assertTrue(result.isSuccess());
        assertEquals("int main(){return 0;}", Files.readString(result.getSourceDir().resolve("main.cpp")));
    }

    @Test void findsNestedCppDirectory() throws Exception {
        Path archive = RegressionFixtures.zipOne(temp, "course/solution/src/main.cpp", "int main(){return 0;}");
        var result = ZipExtractor.extract(archive, temp.resolve("output"));
        assertTrue(result.isSuccess());
        assertEquals("src", result.getSourceDir().getFileName().toString());
    }

    @Test void rejectsZipSlipWithoutWritingOutsideTarget() throws Exception {
        Path archive = RegressionFixtures.zipOne(temp, "../escaped.cpp", "malicious");
        var result = ZipExtractor.extract(archive, temp.resolve("output"));
        assertFalse(result.isSuccess());
        assertFalse(Files.exists(temp.resolve("escaped.cpp")));
    }

    @Test void missingZipIsRejected() {
        var result = ZipExtractor.extract(temp.resolve("missing.zip"), temp.resolve("output"));
        assertFalse(result.isSuccess());
        assertNull(result.getSourceDir());
    }

    @Test void zipWithNoCppIsAcceptedAtExtractionStageInBaseline() throws Exception {
        Path archive = RegressionFixtures.zipOne(temp, "README.txt", "no C++ files");
        var result = ZipExtractor.extract(archive, temp.resolve("output"));
        // Characterization of existing contract; compiler rejects source directories without .cpp.
        assertTrue(result.isSuccess());
        assertEquals(temp.resolve("output"), result.getSourceDir());
    }
}
