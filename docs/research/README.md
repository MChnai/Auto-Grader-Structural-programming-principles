# AutoGrader-X — Research program

Mục tiêu là phát triển một công cụ đánh giá chương trình có tính kiểm chứng khoa học, **CLI-first**, dựa trên AutoGrader V2.

## Sáu hướng nghiên cứu

- **R1 Adaptive Test Generation:** property-based, metamorphic, mutation-guided và cost-aware test selection.
- **R2 Semantic Code Analysis:** AST/CFG, static analysis và đánh giá so với regex.
- **R3 Robust Code Similarity:** Winnowing, AST, CFG và behavior; không tự kết luận đạo văn.
- **R4 Evidence-Grounded Feedback:** nhận diện lỗi, phản hồi bằng chứng và AI tùy chọn có kiểm tra.
- **R5 Secure Reproducible Grading:** sandbox, resource quotas, logs và khả năng replay.
- **R6 Fairness & Reliability:** rubric versioned, agreement với người chấm và độ ổn định.

## Nguyên tắc nghiên cứu

1. Mọi metric cần dataset/seed/baseline/rule đo được ghi vào manifest.
2. Không dùng cùng bài toán cho cả training/tuning và đánh giá cuối mà không kiểm soát rò rỉ.
3. Test không có oracle đủ tin cậy không được dùng để chấm chính thức.
4. Similarity score không là bằng chứng độc lập về hành vi gian lận.
5. Feedback AI không được tự ý đổi điểm và phải phân biệt bằng chứng với giả thuyết.
6. Không tuyên bố thuật toán mới trước khi đối sánh nghiên cứu liên quan.
7. Dữ liệu sinh viên dùng nghiên cứu cần được phép sử dụng và bảo vệ phù hợp.

## Tiến độ

- P00: baseline và repository workflow (P00.1 trong nhánh feature này).
- P01–P03: CLI core, sandbox, evidence và problem spec.
- P04–P09: sáu nhánh nghiên cứu.
- P10–P12: tích hợp, benchmark, công bố/release.

Các hướng trên **chưa được triển khai** trong P00.1.
