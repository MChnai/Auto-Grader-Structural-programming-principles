# Baseline architecture (AutoGrader V2)

Tài liệu phản ánh cây source được nhập từ archive, **không** suy diễn rằng toàn bộ code chạy an toàn hoặc đã được kiểm chứng.

| Package | Thành phần hiện có | Phạm vi |
|---|---|---|
| `archive` | `ZipExtractor` | Giải nén bài nộp và định vị C++ source |
| `executor` | `CompilerEngine`, `ExecutorEngine`, `DockerExecutor`, `LocalExecutor`, `NativeProcessExecutor` | Biên dịch, thực thi và xử lý tiến trình |
| `checker` | `OutputChecker` | So sánh expected/actual output |
| `analyzer` | `CppCodeAnalyzer`, `CodeStyleAnalyzer`, `WinnowingEngine`, ... | Phân tích source và fingerprint |
| `service` | `SubmissionGrader`, `PlagiarismCheckerService` | Điều phối chấm bài và similarity |
| `model` | `GradingReport`, `TestCaseResult`, ... | DTO/model kết quả |
| `cli` | `AutoGraderCommand` | Picocli entrypoint |
| `controller` | `GraderController`, `PlagiarismController` | Spring REST code trong V2 (chưa tách khỏi CLI build) |
| `ui` | `MainFrame`, `liApp` | Legacy stubs, chưa được loại bỏ |

## Known risks / technical debt

1. `SubmissionGrader` gọi native process cho test case, có thể bỏ qua Docker executor; cần sửa trong P02.
2. Biên dịch C++ hiện không được bảo đảm cô lập khỏi host.
3. Rubric của grader chứa trọng số cố định, cần tách trong P09.
4. Analyzer dựa trên heuristic/regex và Winnowing, cần benchmark trước khi đưa kết luận.
5. Cấu trúc CLI, REST và GUI stub hiện cùng tồn tại trong một Maven project.

P00.1 không sửa các vấn đề trên để giữ tính đối sánh với baseline. Ghi kết quả kiểm tra, test và thay đổi tiếp theo theo tài liệu ở `docs/baseline/`.
