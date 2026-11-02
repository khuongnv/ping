# 📋 Nhật Ký Thực Thi: Đổi tên thành "Ping", Thiết kế lại Icon Logo & Gỡ bỏ tính năng Chia sẻ Wi-Fi QR v1.0.3

- **Ngày thực hiện**: 2026-11-02 18:00:00
- **Agent phụ trách**: `lead-architect`, `compose-ui-specialist`, `network-engine-specialist`, `qa-perf-specialist`
- **Yêu cầu gốc**:
  1. Đổi tên ứng dụng thành "Ping".
  2. Gỡ bỏ tính năng chia sẻ QR Wi-Fi (do Android bảo mật không cho ứng dụng bên thứ 3 đọc trích xuất mật khẩu Wi-Fi lưu trên máy).
  3. Thiết kế lại Icon Logo hiện đại, tinh tế theo phong cách Radar / Sonar / Liquid Glass.
  4. Loại bỏ toàn bộ các chữ "VNPT" trong ứng dụng vì ứng dụng phục vụ nhu cầu cá nhân.
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Giải Pháp Kỹ Thuật

1. **Rào cản trích xuất mật khẩu Wi-Fi trên Android**:
   - Từ Android 8 trở đi và đặc biệt từ Android 10 - 15, Android SELinux sandbox và Wi-Fi Framework ngăn chặn hoàn toàn các ứng dụng bên thứ ba (non-system app, `UID != 1000`) truy cập trường mật khẩu `WifiConfiguration.preSharedKey` (hàm này luôn trả về `"*"` hoặc `null`). Chỉ có cài đặt hệ thống của nhà sản xuất (OEM Settings) hoặc thiết bị đã Root mới có thể đọc được mật khẩu này.
   - Do đó, việc chia sẻ Wi-Fi qua QR code mà không có mật khẩu trở nên không hữu dụng đối với người dùng cá nhân. Quyết định: gỡ bỏ hoàn toàn tính năng này và thư viện ZXing để ứng dụng nhẹ nhàng, thuần túy là công cụ mạng chuyên sâu.

2. **Đổi tên ứng dụng & Loại bỏ thương hiệu VNPT**:
   - Tên ứng dụng chính thức: **Ping**.
   - Chuỗi `app_name` trong `strings.xml` được đổi thành `"Ping"`.
   - Tiêu đề màn hình `SettingsScreen` đổi thành `"Ping (Zero Ads Edition)"`.
   - Host mặc định trong `PingScreen` được thay thế bằng `"cloudflare.com"`.
   - `NetworkInfoService` loại bỏ tiền tố VNPT trong ISP fallback.
   - Giữ nguyên `applicationId = "com.vnpt.pingtool"` để tránh bị xung đột package name hoặc làm mất khả năng update trên thiết bị người dùng.

3. **Thiết kế lại Icon Logo (Apple Music HIG x Liquid Glass Sonar Radar)**:
   - **Background**: Nền đêm huyền bí `#09090D` với quầng sáng tỏa tròn Gradient và viền phản quang bo tròn Liquid Glass Rim mờ ảo.
   - **Foreground**:
     - Tâm phát sóng (Beacon Core) màu trắng phát sáng rực rỡ với vầng hào quang tím hồng (Neon Magenta `#FA2D55`).
     - 3 vòng cung Radar / Sonar mở rộng dạng sóng quét mạng lưới không gian.
     - Dải sóng điện tâm đồ mạng (Network Latency ECG Waveform) nằm ngang với các đỉnh xung nhọn biểu trưng cho tốc độ phản hồi cực nhanh (low-latency ping).
   - Tự động sinh đầy đủ các độ phân giải `mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi` cho cả `ic_launcher.png` và `ic_launcher_round.png`.

---

## 2. Chi Tiết Các Bước Đã Xử Lý (Execution Steps)

- **Bước 1**: Đổi tên hiển thị ứng dụng trong `strings.xml` thành `Ping`.
- **Bước 2**: Rà soát và loại bỏ tất cả các chuỗi "VNPT" trong giao diện người dùng và logic nội bộ.
- **Bước 3**: Xóa bỏ hoàn toàn tính năng QR Wi-Fi:
  - Xóa `WifiQrShareDialog.kt` và `WifiQrManager.kt`.
  - Gỡ bỏ thư viện `zxing:core` trong `app/build.gradle.kts` và `gradle/libs.versions.toml`.
  - Làm gọn `MyIpScreen.kt`: Bỏ nút mở dialog QR, giữ nguyên tính năng cấp quyền vị trí 1 chạm để lấy tên SSID chuẩn.
- **Bước 4**: Tạo vector drawable và render bộ icon mới đẹp mắt chuẩn Android Adaptive Icon.
- **Bước 5**: Nâng cấp phiên bản trong `app/build.gradle.kts`: `versionCode = 4`, `versionName = "1.0.3"`.
- **Bước 6**: Biên dịch bản Release APK có ký số V1 & V2 và phát hành lên GitHub Release.

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)

- `[Deleted]` `app/src/main/java/com/vnpt/pingtool/ui/components/WifiQrShareDialog.kt`
- `[Deleted]` `app/src/main/java/com/vnpt/pingtool/data/network/WifiQrManager.kt`
- `[Modified]` `app/build.gradle.kts` - Bỏ dependency zxing, nâng `versionCode = 4`, `versionName = "1.0.3"`
- `[Modified]` `gradle/libs.versions.toml` - Bỏ thư viện zxing
- `[Modified]` `app/src/main/res/values/strings.xml` - Đổi `app_name` thành `"Ping"`
- `[Modified]` `app/src/main/AndroidManifest.xml` - Cập nhật label ứng dụng
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/MyIpScreen.kt` - Gỡ bỏ QR code UI
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/PingScreen.kt` - Đổi host mặc định sang cloudflare.com
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/SettingsScreen.kt` - Đổi tên app thành Ping
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/network/NetworkInfoService.kt` - Gỡ bỏ chuỗi mặc định VNPT
- `[Modified]` `app/src/main/res/drawable/ic_launcher_background.xml` - Background logo mới
- `[Modified]` `app/src/main/res/drawable/ic_launcher_foreground.xml` - Foreground logo Radar Sonar mới
- `[Modified]` `app/src/main/res/mipmap-*/ic_launcher*.png` - Render đầy đủ 10 icon PNG theo kích thước chuẩn Android
- `[Created]` `docs/logs/2026-11-02_rename_to_ping_and_redesign_icon.md` - Nhật ký thực thi phiên làm việc này

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- Mã nguồn biên dịch thành công không còn phụ thuộc ZXing.
- Bản dựng Release `PingTool-v1.0.3.apk` đã được ký số APK Signature Scheme v2 và tải trực tiếp lên GitHub Release:
  `https://github.com/khuongnv/ping/releases/tag/v1.0.3`
