package vn.edu.dlu.autograder.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
     * BỔ SUNG: Hàm extractZip đơn giản để giải nén trực tiếp vào thư mục chỉ định (void)
     */
    public static void extractZip(Path zipFilePath, Path destinationDir) throws IOException {
        ExtractResult result = extract(zipFilePath, destinationDir);
        if (!result.isSuccess()) {
            throw new IOException(result.getMessage());
        }
    }

    /**
     * Giải nén file .zip vào destinationDir và tự động định vị thư mục chứa code .cpp
     */
    public static ExtractResult extract(Path zipFilePath, Path destinationDir) {
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
                // Nếu không tìm thấy .cpp, trả về chính thư mục đích để gom file chung
                return new ExtractResult(true, "Giải nén thành công!", destinationDir);
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
    private static Path findCppSourceDirectory(Path rootDir) throws IOException {
        try (Stream<Path> stream = Files.walk(rootDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null && 
                                 p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cpp"))
                    .map(Path::getParent)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * BỔ SUNG HOÀN CHỈNH: Hàm dọn dẹp xóa sạch toàn bộ thư mục tạm và file con bên trong
     */
    public static void cleanUpDirectory(Path directoryPath) {
        if (directoryPath == null || !Files.exists(directoryPath)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(directoryPath)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            System.err.println("[Cảnh báo] Không thể xóa triệt để thư mục tạm: " + directoryPath);
        }
    }
}