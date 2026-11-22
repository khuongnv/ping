# 📋 Bảng Kiểm Tra Trước Khi Phát Hành Phiên Bản (Release Checklist)

Tài liệu kiểm tra từng bước dành cho `qa-perf-specialist` và `lead-architect` trước khi công bố phiên bản APK mới cho người dùng.

---

## 🔍 Checklist Kiểm Tra

### 1. Kiểm tra mã nguồn & Trạng thái Git
- [ ] `git status` sạch, không còn file tạm hoặc file chưa commit cần thiết.
- [ ] Nhánh hiện tại là `main` (hoặc nhánh release chỉ định).
- [ ] Các tính năng mới đều tuân thủ nguyên tắc **Zero Ads 100%**.

### 2. Kiểm tra chất lượng & Biên dịch (Build Verification)
- [ ] Chạy lệnh kiểm tra biên dịch thành công:
  ```powershell
  .\gradlew.bat compileDebugKotlin
  ```
- [ ] Kiểm tra build APK release nội bộ:
  ```powershell
  .\gradlew.bat assembleRelease
  ```
- [ ] Không có warning nghiêm trọng làm crash runtime.

### 3. Kiểm tra tính năng mạng & Giao diện Liquid Glass
- [ ] Thao tác Ping: Bắt đầu / Dừng Ping phản hồi mượt mà, không rò rỉ tiến trình nền.
- [ ] Biểu đồ sóng: Duy trì 60/120 FPS, đổi màu chuẩn theo ngưỡng RTT (<50ms Xanh, 50-150ms Vàng, >150ms Đỏ).
- [ ] Màn hình My IP: Hiển thị đúng IP Wi-Fi, Gateway, DNS và Public IP.
- [ ] Cơ chế cập nhật: Nút kiểm tra bản cập nhật trong Settings trỏ đúng URL API `https://api.github.com/repos/khuongnv/ping/releases/latest`.

### 4. Triển khai Release
- [ ] Khởi tạo tệp log phát hành trong `docs/logs/`.
- [ ] Thực thi script deploy:
  ```powershell
  powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-app/scripts/deploy.ps1 -BumpType patch -ReleaseTitle "<Tiêu đề bản phát hành>"
  ```
- [ ] Giám sát tiến trình build trên tab GitHub Actions cho đến khi hoàn thành xuất bản file `PingTool-vX.Y.Z.apk`.
