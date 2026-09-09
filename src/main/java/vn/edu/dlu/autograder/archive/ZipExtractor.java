package vn.edu.dlu.autograder.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.stream.Stream;
import java.util.Locale;

public class ZipExtractor {

    public static class ExtractResult {
        private final boolean success;
        private final String message;
        private final Path sourceDir; // Thư mục thực sự chứa các file .cpp

        public ExtractResult(boolean success, String message, Path sourceDir) {
            this.success = success;
            this.message = message;
            this.sourceDir = sourceDir;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Path getSourceDir() { return sourceDir; }
    }

    /**
     * Giải nén file .zip vào destinationDir và tự động định vị thư mục chứa code .cpp
     */
    public ExtractResult extract(Path zipFilePath, Path destinationDir) {
        if (zipFilePath == null || !Files.exists(zipFilePath)) {
            return new ExtractResult(false, "Lỗi: File .zip không tồn tại!", null);
        }

        try {
            // 1. Tự động tạo thư mục đích nếu chưa có
            Files.createDirectories(destinationDir);

            // 2. Tiến hành giải nén (Sử dụng mã hóa UTF-8)
            try (InputStream fis = Files.newInputStream(zipFilePath);
                 ZipInputStream zis = new ZipInputStream(fis, StandardCharsets.UTF_8)) {

                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    // Chống lỗi Zip Slip (Path Traversal)
                    Path targetPath = destinationDir.resolve(entry.getName()).normalize();
                    if (!targetPath.startsWith(destinationDir.normalize())) {
                        return new ExtractResult(false, "Cảnh báo an ninh: File zip chứa đường dẫn độc hại (Zip Slip Attack)!", null);
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        // Tạo các thư mục cha nếu chưa tồn tại
                        if (targetPath.getParent() != null) {
                            Files.createDirectories(targetPath.getParent());
                        }

                        // Ghi dữ liệu file ra đĩa
                        try (OutputStream os = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            zis.transferTo(os);
                        }
                    }
                    zis.closeEntry();
                }
            }

            // 3. Tự động định vị thư mục thực sự chứa các file .cpp (Xử lý cấu trúc folder lồng nhau)
            Path actualSourceDir = findCppSourceDirectory(destinationDir);
            if (actualSourceDir == null) {
                return new ExtractResult(false, "Lỗi: Không tìm thấy bất kỳ file .cpp nào trong file .zip đã giải nén!", null);
            }

            return new ExtractResult(true, "Giải nén thành công!", actualSourceDir);

        } catch (IOException e) {
            return new ExtractResult(false, "Lỗi I/O khi giải nén file zip: " + e.getMessage(), null);
        } catch (Exception e) {
            return new ExtractResult(false, "Lỗi hệ thống không xác định khi giải nén: " + e.getMessage(), null);
        }
    }

    /**
     * Quét thư mục để tìm thư mục nông nhất chứa ít nhất 1 file .cpp
     */
    private Path findCppSourceDirectory(Path rootDir) throws IOException {
        try (Stream<Path> stream = Files.walk(rootDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null && 
                                 p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cpp"))
                    .map(Path::getParent)
                    .findFirst()
                    .orElse(null);
        }
    }
}