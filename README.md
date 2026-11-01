# AutoGrader-X — research baseline (P00.1)

AutoGrader-X kế thừa **AutoGrader V2**, một công cụ Java hỗ trợ chấm bài nộp C/C++ bằng test case, phân tích một số quy tắc mã nguồn và so sánh tương đồng dựa trên Winnowing. Phiên bản này hiện mới thực hiện **P00.1 — làm sạch repository và chuẩn hóa quy trình Git**; tên AutoGrader-X thể hiện hướng phát triển, **chưa** đồng nghĩa sáu hướng nghiên cứu đã được triển khai.

## Tình trạng repository

- `main`: snapshot mã nguồn AutoGrader V2 đã nhập, không sửa logic nghiệp vụ.
- `develop`: nhánh được tạo để dùng cho việc tích hợp sau này; hiện chưa merge bất cứ thay đổi nào.
- `feature/AX-P00-repository-baseline`: sửa `.gitignore`, bổ sung tài liệu, kiểm tra hygiene và CI.
- `baseline/autograderv2`: annotated tag trỏ tới commit snapshot nguồn đã nhập (không gồm build artifacts).

**Lưu ý:** File ZIP ban đầu không có metadata Git hoặc GitHub remote. Lịch sử trong bản phân phối này là lịch sử Git **cục bộ mới tạo**, không phải lịch sử repository GitHub gốc. Không có thao tác `git pull`, push, tạo PR hoặc merge trên GitHub được thực hiện từ bản ZIP. Xem `docs/repository-workflow.md` để thao tác trên repository thật.

## Yêu cầu môi trường

- JDK 21, Maven 3.x.
- Trình biên dịch C++ `g++` trong PATH để chạy `CompilerEngineTest` và một số ca kiểm thử tích hợp.
- Docker Desktop hoặc Docker Engine cho các chức năng liên quan Docker (khi sử dụng).
- Git để quản lý nhánh, tag và tạo PR.

## Build và kiểm tra baseline

```bash
java -version
mvn -version
g++ --version
mvn -B -ntp clean verify
python scripts/check_repo_hygiene.py
```

Lệnh chạy JAR CLI **sau khi build package thành công**:

```bash
java -jar target/autograder-1.0.0-cli.jar --help
java -jar target/autograder-1.0.0-cli.jar grade --help
java -jar target/autograder-1.0.0-cli.jar check-plagiarism --help
```

Chưa có kết quả `mvn verify` được xác nhận trong môi trường tạo bản phát hành P00.1 vì không có Maven. Workflow `.github/workflows/baseline-ci.yml` sẽ chạy build/test khi repository được đưa lên GitHub; kết quả CI mới là bằng chứng build trong môi trường đó.

> **Cảnh báo an toàn:** Luồng chấm AutoGrader V2 còn thực thi tiến trình native trên host. Không biên dịch/chạy bài nộp không tin cậy trên máy chứa dữ liệu quan trọng. P02 sẽ thay bằng sandbox thực sự và bổ sung security tests.

## Hướng phát triển

Sáu nhánh nghiên cứu dự kiến: adaptive test generation, AST/CFG analysis, robust code similarity, verified feedback, secure reproducible grading, grading fairness/reliability. Xem `docs/research/README.md`.

## Quy trình đóng góp

Không sửa trực tiếp hoặc merge `main`. Luôn `git switch main && git pull --ff-only origin main` trên bản clone GitHub, sau đó tạo `feature/*` hoặc `research/*`, commit, push và tạo Pull Request. Chi tiết tại `docs/repository-workflow.md`.

## Nội dung repo

- `src/main/java/`: ứng dụng Java V2.
- `src/test/java/`: unit/integration tests gốc.
- `test_data/`: test fixtures, bao gồm `.zip`, `.in`, `.out` cần giữ lại.
- `docs/architecture/`: phân tích kiến trúc baseline.
- `docs/research/`: câu hỏi nghiên cứu và kế hoạch.
- `experiments/`: protocol, scripts và metadata thực nghiệm (chưa có kết quả chạy).
- `scripts/check_repo_hygiene.py`: tự kiểm tra các quy tắc P00.1.
