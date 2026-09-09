package vn.edu.dlu.autograder.archive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Module 0: ZipExtractor (Giải nén & An toàn hệ thống)")
public class ZipExtractorTest {

    private ZipExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ZipExtractor();
    }

    @Test
    @DisplayName("1. Giải nén file Zip hợp lệ chứa file .cpp (Happy Path)")
    void testExtractValidZip(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("submission.zip");
        createDummyZip(zipFile, "main.cpp", "#include <iostream>\nint main(){ return 0; }");

        Path extractTarget = tempDir.resolve("extracted");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        assertTrue(result.isSuccess(), "Giải nén phải thành công");
        assertNotNull(result.getSourceDir());
        assertTrue(Files.exists(result.getSourceDir().resolve("main.cpp")), "File main.cpp phải thực sự tồn tại trên ổ đĩa");
    }

    @Test
    @DisplayName("2. Tự động định vị file .cpp nằm trong thư mục con lồng nhau")
    void testNestedFolderStructure(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("nested_submission.zip");
        createDummyZip(zipFile, "Project/src/student_code.cpp", "// Code cpp trong folder con");

        Path extractTarget = tempDir.resolve("extracted_nested");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        assertTrue(result.isSuccess());
        assertNotNull(result.getSourceDir());
        assertEquals("src", result.getSourceDir().getFileName().toString(), "Phải định vị đúng thư mục 'src' chứa file .cpp");
    }

    @Test
    @DisplayName("3. File Zip không chứa bất kỳ file .cpp nào")
    void testZipWithoutCppFiles(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("no_cpp.zip");
        createDummyZip(zipFile, "readme.txt", "Day la file text khong phai code C++");

        Path extractTarget = tempDir.resolve("extracted_no_cpp");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        assertFalse(result.isSuccess());
        assertNull(result.getSourceDir(), "Contract: sourceDir phải trả về null khi không tìm thấy code");
        assertTrue(result.getMessage().contains("Không tìm thấy bất kỳ file .cpp nào"));
    }

    @Test
    @DisplayName("4. Xử lý khi đường dẫn file .zip không tồn tại")
    void testNonExistentZipFile(@TempDir Path tempDir) {
        Path fakeZipPath = tempDir.resolve("not_found.zip");
        ZipExtractor.ExtractResult result = extractor.extract(fakeZipPath, tempDir.resolve("target"));

        assertFalse(result.isSuccess());
        assertNull(result.getSourceDir());
        assertTrue(result.getMessage().contains("không tồn tại"));
    }

    @Test
    @DisplayName("5. Từ chối và ngăn chặn tấn công Zip Slip (Path Traversal)")
    void testZipSlipAttack(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("malicious.zip");
        // Giả lập file zip chứa đường dẫn nguy hiểm cố tình thoát khỏi thư mục đích
        createDummyZip(zipFile, "../evil.cpp", "malicious content");

        Path extractTarget = tempDir.resolve("extracted");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        // 1. Kiểm tra trạng thái trả về
        assertFalse(result.isSuccess(), "Phải từ chối giải nén khi phát hiện Zip Slip");
        assertNull(result.getSourceDir());
        assertTrue(result.getMessage().contains("Zip Slip Attack"), "Thông báo phải cảnh báo Zip Slip");

        // 2. MẮT XÍCH BẢO MẬT QUAN TRỌNG: Đảm bảo file độc hại KHÔNG ĐƯỢC GHI ra ổ đĩa
        assertFalse(Files.exists(tempDir.resolve("evil.cpp")), "Bảo mật: File độc hại tuyệt đối không được xuất hiện ngoài extractTarget!");
    }

    @Test
    @DisplayName("6. Nhận diện file .CPP viết hoa không phân biệt chữ hoa/thường")
    void testUpperCaseCppExtension(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("uppercase.zip");
        createDummyZip(zipFile, "MAIN.CPP", "int main() {}");

        Path extractTarget = tempDir.resolve("extracted");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        assertTrue(result.isSuccess());
        assertNotNull(result.getSourceDir());
        assertTrue(Files.exists(result.getSourceDir().resolve("MAIN.CPP")));
    }

    @Test
    @DisplayName("7. File Zip rỗng không chứa bất kỳ entry nào")
    void testEmptyZip(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("empty.zip");
        
        // Tạo file ZIP rỗng hoàn toàn
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // Không ghi entry nào
        }

        Path extractTarget = tempDir.resolve("extracted_empty");
        ZipExtractor.ExtractResult result = extractor.extract(zipFile, extractTarget);

        assertFalse(result.isSuccess());
        assertNull(result.getSourceDir());
        assertTrue(result.getMessage().contains("Không tìm thấy bất kỳ file .cpp nào"));
    }

    /**
     * Hàm hỗ trợ tạo file .zip giả lập trực tiếp trong bộ nhớ để phục vụ Test
     */
    private void createDummyZip(Path zipPath, String entryName, String content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}