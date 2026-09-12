package vn.edu.dlu.autograder.analyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlagiarismReportExporter {

    public static class MatchResult {
        private final String student1;
        private final String student2;
        private final double similarity;

        public MatchResult(String student1, String student2, double similarity) {
            this.student1 = student1;
            this.student2 = student2;
            this.similarity = similarity;
        }

        public String getStudent1() { return student1; }
        public String getStudent2() { return student2; }
        public double getSimilarity() { return similarity; }
    }

    public static void exportToHtml(List<MatchResult> results, double threshold, File outputFile) throws IOException {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"vi\">\n")
            .append("<head>\n")
            .append("  <meta charset=\"UTF-8\">\n")
            .append("  <title>Báo Cáo Kiểm Tra Đạo Văn</title>\n")
            .append("  <style>\n")
            .append("    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8f9fa; margin: 30px; color: #333; }\n")
            .append("    .container { max-width: 1000px; margin: 0 auto; background: #fff; padding: 25px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n")
            .append("    h1 { color: #2c3e50; border-bottom: 2px solid #007bff; padding-bottom: 10px; }\n")
            .append("    .info { margin-bottom: 20px; color: #6c757d; font-size: 14px; }\n")
            .append("    table { width: 100%; border-collapse: collapse; margin-top: 20px; }\n")
            .append("    th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #dee2e6; }\n")
            .append("    th { background-color: #007bff; color: white; text-transform: uppercase; font-size: 13px; }\n")
            .append("    tr:hover { background-color: #f1f1f1; }\n")
            .append("    .badge { padding: 5px 10px; border-radius: 4px; font-weight: bold; color: white; display: inline-block; }\n")
            .append("    .bg-danger { background-color: #dc3545; }\n")
            .append("    .bg-warning { background-color: #ffc107; color: #212529; }\n")
            .append("    .bg-success { background-color: #28a745; }\n")
            .append("    .no-data { text-align: center; padding: 20px; color: #28a745; font-size: 16px; font-weight: bold; }\n")
            .append("  </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("  <div class=\"container\">\n")
            .append("    <h1>BÁO CÁO KIỂM TRA ĐẠO VĂN</h1>\n")
            .append("    <div class=\"info\">\n")
            .append("      <p><strong>Thời gian xuất báo cáo:</strong> ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))).append("</p>\n")
            .append("      <p><strong>Ngưỡng cảnh báo:</strong> ").append(String.format("%.1f%%", threshold)).append("</p>\n")
            .append("    </div>\n");

        if (results.isEmpty()) {
            html.append("    <div class=\"no-data\">Không phát hiện cặp bài nộp nào vượt ngưỡng nghi vấn.</div>\n");
        } else {
            html.append("    <table>\n")
                .append("      <thead>\n")
                .append("        <tr>\n")
                .append("          <th>#</th>\n")
                .append("          <th>Sinh viên A</th>\n")
                .append("          <th>Sinh viên B</th>\n")
                .append("          <th>Mức độ tương đồng</th>\n")
                .append("          <th>Đánh giá</th>\n")
                .append("        </tr>\n")
                .append("      </thead>\n")
                .append("      <tbody>\n");

            int index = 1;
            for (MatchResult res : results) {
                String badgeClass = res.getSimilarity() >= 70.0 ? "bg-danger" : (res.getSimilarity() >= 50.0 ? "bg-warning" : "bg-success");
                String statusText = res.getSimilarity() >= 70.0 ? "Nghi vấn cao" : (res.getSimilarity() >= 50.0 ? "Cảnh báo" : "An toàn");

                html.append("        <tr>\n")
                    .append("          <td>").append(index++).append("</td>\n")
                    .append("          <td><strong>").append(res.getStudent1()).append("</strong></td>\n")
                    .append("          <td><strong>").append(res.getStudent2()).append("</strong></td>\n")
                    .append("          <td><span class=\"badge ").append(badgeClass).append("\">").append(String.format("%.2f%%", res.getSimilarity())).append("</span></td>\n")
                    .append("          <td>").append(statusText).append("</td>\n")
                    .append("        </tr>\n");
            }

            html.append("      </tbody>\n")
                .append("    </table>\n");
        }

        html.append("  </div>\n")
            .append("</body>\n")
            .append("</html>");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }
    }
}