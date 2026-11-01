# Changelog

Ghi nhận những thay đổi được thực hiện trong AutoGrader-X. Changelog theo từng milestone kỹ thuật; không mặc định một chức năng hoàn thiện nếu chưa có test và minh chứng.

## [Unreleased] — P00.1 Repository baseline

### Added
- Tài liệu khởi tạo, kiến trúc, định hướng nghiên cứu và quy trình branch/PR.
- `scripts/check_repo_hygiene.py` để kiểm tra nguồn và các file được Git theo dõi.
- GitHub Actions workflow cho Java 21 + Maven baseline verification.
- Pull Request template và mã SHA-256 của ZIP nguồn.
- `docs/baseline/README.md` cho thông tin snapshot và các giới hạn kiểm chứng.

### Changed
- Làm sạch `.gitignore`, loại dòng `>>>>>>> feature/rest-api-controllers`.
- Bổ sung ignore cho `target/`, `.class`, `.jar`, IDE files và kết quả thực nghiệm sinh ra; không bỏ các fixture `.zip`, `.out`.

### Unchanged
- Không sửa nghiệp vụ Java, Maven POM hoặc test V2 trong P00.1.
- Không merge `main`/`develop`, không push hoặc tạo PR từ bản ZIP.

## [baseline/autograderv2] — Original imported snapshot

- Snapshot source Java và test fixtures từ `AutoGraderv2.zip`.
- Bỏ qua file build đã được sinh trước đó (không xem là source).
- Các vấn đề kiến trúc và an toàn hiện tại được ghi nhận; chưa tuyên bố đã được sửa.


## P00.3 — Regression tests (unreleased)

### Added
- JUnit 5 regression tests for eight existing components.
- AC, WA, CE, RE and TLE regression scenarios.
- Known-defect registry based on P00.2.
- Cross-platform regression fixtures.

### Unchanged
- Production Java source and pom.xml remain unchanged.
- Known defects have not been fixed.
- No local merge into main or develop.