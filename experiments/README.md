# Experiments workspace

Thư mục này quản lý cấu hình, script và giao thức thí nghiệm khoa học. P00.1 chỉ tạo cấu trúc; **chưa có kết quả thực nghiệm**.

Quy tắc:

- Commit protocol, config, scripts, dataset manifest và kết quả tổng hợp được phép công bố.
- Không commit output nặng hoặc nhạy cảm trong `experiments/results/` và `experiments/runs/`.
- Một thí nghiệm phải có vấn đề nghiên cứu, baseline, chỉ số, dữ liệu, phiên bản môi trường và seed.
- Báo cáo thiếu kết quả không được tự đánh dấu pass; lưu nguyên nhân lỗi.
- Chỉ công bố dữ liệu bài nộp được cấp quyền và đã xử lý thông tin cá nhân phù hợp.

Các file `test_data/**/*.zip` và `.out` là **test fixtures của source**, không phải build artifacts; không được xóa hoặc ignore hàng loạt.
