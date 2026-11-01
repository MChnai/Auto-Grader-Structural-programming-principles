# Baseline AutoGrader V2 — P00.1

## Nguồn gốc

- Nguồn nhập: `AutoGraderv2.zip` do người sử dụng cung cấp.
- ZIP nguồn không chứa `.git`, remote URL hoặc thông tin branch GitHub.
- Tag `baseline/autograderv2` trỏ tới commit nhập snapshot nguồn được chuẩn hóa để chỉ bao gồm mã nguồn/test fixtures; không gồm `target/`, file IDE cục bộ hoặc generated `dependency-reduced-pom.xml`.
- SHA-256 của file ZIP gốc: xem `docs/baseline/source-archive-sha256.txt` (SHA-256 của archive gốc). Giá trị này dùng để đối chiếu archive, không xác nhận lịch sử Git ngoài GitHub.

## Phạm vi đã thực hiện

- Đã làm sạch `.gitignore` trên feature branch.
- Đã thêm tài liệu và kiểm tra repository hygiene.
- Đã thiết lập workflow CI để yêu cầu build/test trên GitHub.
- Chưa có việc điều chỉnh thuật toán, sandbox, API, grader hoặc module kiểm thử.

## Môi trường và giới hạn

- Runtime Java tại môi trường đóng gói: OpenJDK 21.
- Maven không có sẵn tại môi trường đóng gói: trạng thái build/test **NOT RUN**, không được ghi là PASS.
- Không có Git remote trong ZIP: `git pull`, `git push`, GitHub PR và remote creation **NOT PERFORMED**.
- Mã nguồn được giữ nguyên trong commit baseline và trong feature commit, ngoại trừ file tài liệu/cấu hình Git/CI mới.

## Việc tiếp theo

P00.2: kiểm tra build/test trên máy có Maven + compiler, ghi version và log. P00.3: bổ sung regression fixture theo các lỗi AC/WA/CE/TLE/RE, kiểm tra deadlock và cleanup. P02: sửa đường thực thi không an toàn. Không gắn trạng thái hoàn tất cho các phần chưa được kiểm chứng.

## P00.2 — Functional baseline audit

See [`P00.2-runbook.md`](P00.2-runbook.md) for command-line reproduction and [`P00.2-functional-audit.md`](P00.2-functional-audit.md) for the recorded results. This stage deliberately preserves source behavior, including known deficiencies. P00.3 adds long-lived regression tests; P02+ fixes the findings.
