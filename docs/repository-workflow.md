# Git workflow — MAIN → FEATURE → PULL REQUEST (no local merge)

## 1. Làm việc với repository GitHub thật

> Bản ZIP gốc không chứa `.git`/`origin`; các lệnh sau phải chạy trong **clone GitHub thật**. Không có lệnh nào ở đây tự động được thực thi trên GitHub trong P00.1.

```bash
# Trong local clone có remote origin đã cấu hình đúng:
git status --short
# Nếu có thay đổi chưa commit, commit/stash ra nhánh riêng trước khi switch.
git switch main
git pull --ff-only origin main

# Nếu muốn gắn tag snapshot V2 trên main và tag chưa tồn tại:
git tag -a baseline/autograderv2 -m "AutoGrader V2 baseline before P00 modernization"
# Đẩy tag riêng sau khi xác nhận đúng commit và có quyền:
git push origin baseline/autograderv2

# Tạo feature branch từ main vừa pull:
git switch -c feature/AX-P00-repository-baseline
```

Nếu tag đã tồn tại, kiểm tra `git show baseline/autograderv2` và **không di chuyển/ghi đè tag**. Nếu main đã chứa thay đổi chưa đối chiếu với bản ZIP, hãy chọn commit baseline chính xác thay vì tag nhầm `main` hiện tại.

## 2. Áp dụng bản patch P00.1 lên clone hiện có

Khi nhận `autograder-x-P00.1.patch` từ gói phát hành, đặt file cạnh repo hoặc dùng đường dẫn tuyệt đối:

```bash
# Chỉ áp dụng trên feature branch, sau khi pull main.
git apply --check ../autograder-x-P00.1.patch
git apply ../autograder-x-P00.1.patch
python scripts/check_repo_hygiene.py
mvn -B -ntp clean verify

git add .gitignore .github README.md CHANGELOG.md docs experiments scripts
git diff --cached --check
git commit -m "[AX-P00] Clean repository and document research baseline"
git push -u origin feature/AX-P00-repository-baseline
```

Patch chỉ thay `.gitignore` và thêm tài liệu/script/CI, không sửa source Java hay POM. Nếu `git apply --check` lỗi do repo GitHub đã khác snapshot ZIP, **không dùng `--force`/`-3` tự động**: so sánh, giải quyết thủ công trên feature branch và kiểm tra lại.

## 3. Pull Request trên GitHub

- Base: `main` (hoặc theo quy ước maintainer của repository thực tế).
- Compare: `feature/AX-P00-repository-baseline`.
- Chạy CI và review trước khi quyết định merge.
- **Không chạy `git merge`, `git rebase` vào main/develop hoặc push trực tiếp lên main như một phần của P00.1.**
- Việc merge PR là hành động riêng trên GitHub do người quản lý repository thực hiện.

## 4. Branch policy đề xuất

| Branch | Vai trò |
|---|---|
| `main` | Baseline/release được bảo vệ; merge bằng PR khi phê duyệt |
| `develop` | Tùy chọn cho tích hợp các feature lớn, không tự merge |
| `feature/AX-Pxx-...` | Tính năng và sửa lỗi theo roadmap |
| `research/RQx-...` | Thí nghiệm thuật toán, kết quả và ablation |

Không tạo nhánh rỗng chỉ để minh họa `feature/*`, `research/*`. Khuyến nghị bảo vệ `main` với PR + CI, không cho force-push, tùy quyền và chính sách GitHub.

## 5. Phương án dùng Git bundle

Bản phát hành còn có `.bundle` chứa `main`, `develop`, feature branch và annotated tag trong **repo cục bộ được tạo từ ZIP**. Git bundle hữu ích để xem lịch sử độc lập, nhưng không thay thế `git pull` từ GitHub thật và không được push đè lên các nhánh remote đang tồn tại.
