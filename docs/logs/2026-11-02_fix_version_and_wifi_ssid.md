# 📋 Nhật Ký Thực Thi: Khắc phục hiển thị phiên bản & Tên mạng Wi-Fi (SSID) v1.0.2

- **Ngày thực hiện**: 2026-11-02 17:48:00
- **Agent phụ trách**: `lead-architect`, `compose-ui-specialist`, `device-network-specialist`
- **Yêu cầu gốc**: Sửa lỗi hiển thị phiên bản v1.0.0 gây lặp thông báo update và khắc phục hiển thị tên mạng Wi-Fi (SSID)
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Nguyên Nhân Gốc (Root Cause Analysis)
1. **Lỗi phiên bản bị gán cứng (Hardcoded Version Name)**:
   - Trong `GitHubUpdateManager.kt` và `SettingsScreen.kt`, giá trị `currentVersionName` bị gán cứng là `"1.0.0"`.
   - Khi ứng dụng so sánh với bản phát hành mới nhất từ GitHub (`v1.0.1`), kết quả luôn là `1.0.1 > 1.0.0` dẫn tới việc ứng dụng luôn hiển thị có bản cập nhật mới ngay cả khi người dùng đã cài đặt bản mới nhất.
2. **Lỗi không hiển thị tên Wi-Fi (SSID) trên Android**:
   - Từ Android 8.1 / 9 / 10+ trở lên, Google đưa SSID vào danh mục dữ liệu vị trí nhạy cảm (Location-identifying data). Để trích xuất được SSID từ `WifiInfo`:
     - Ứng dụng bắt buộc phải khai báo các quyền `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` và `NEARBY_WIFI_DEVICES`.
     - Ứng dụng phải yêu cầu quyền tại thời điểm chạy (Runtime Permission). Trước đó trong `AndroidManifest.xml` không hề khai báo quyền vị trí nào, khiến Android luôn trả về `"<unknown ssid>"`.

## 2. Chi Tiết Các Bước Đã Xử Lý (Execution Steps)
- **Bước 1**: Thay thế toàn bộ phiên bản gán cứng bằng cơ chế đọc động từ Package Info:
  `context.packageManager.getPackageInfo(context.packageName, 0).versionName`.
- **Bước 2**: Khai báo các quyền vị trí trong `AndroidManifest.xml`:
  `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `NEARBY_WIFI_DEVICES`.
- **Bước 3**: Nâng cấp `NetworkInfoService.kt` hỗ trợ trích xuất Wi-Fi SSID trên cả Android 10+ (`NetworkCapabilities.transportInfo`) và Android đời cũ. Nếu chưa có quyền vị trí, trả về trạng thái chỉ dẫn rõ ràng.
- **Bước 4**: Tích hợp `permissionLauncher` trong `MyIpScreen.kt`:
  - Khi thiết bị chưa cấp quyền vị trí, hiển thị thanh hướng dẫn màu vàng hổ phách: `Chạm để cấp quyền vị trí giúp hiển thị tên Wi-Fi`.
  - Bấm vào sẽ mở hộp thoại xin quyền chuẩn của Android. Khi người dùng bấm cho phép, tên Wi-Fi sẽ xuất hiện ngay lập tức.
- **Bước 5**: Tối ưu `WifiQrShareDialog.kt` cho phép người dùng tự nhập hoặc chỉnh sửa tên SSID nếu điện thoại không tự nhận diện được.
- **Bước 6**: Bump phiên bản lên `v1.0.2` (Build 3) có ký số hoàn chỉnh và xuất bản trực tiếp lên GitHub Releases.

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/build.gradle.kts` - Nâng `versionCode = 3`, `versionName = "1.0.2"`
- `[Modified]` `app/src/main/AndroidManifest.xml` - Khai báo quyền vị trí & nearby devices
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/model/Models.kt` - Bổ sung cờ `isWifiTransport` và `needsLocationPermission`
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/network/NetworkInfoService.kt` - Đọc SSID động và kiểm tra quyền
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/update/GitHubUpdateManager.kt` - Đọc phiên bản động từ PackageManager
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/SettingsScreen.kt` - Hiển thị phiên bản động
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/MyIpScreen.kt` - Nút cấp quyền vị trí 1 chạm
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/components/WifiQrShareDialog.kt` - Cho phép nhập/sửa SSID thủ công
- `[Created]` `docs/logs/2026-11-02_fix_version_and_wifi_ssid.md` - Nhật ký thực thi phiên làm việc này

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- Biên dịch release APK thành công với Gradle (`assembleRelease`).
- File `PingTool-v1.0.2.apk` đã được ký số APK Signature Scheme v2 và tải trực tiếp lên GitHub Release:
  `https://github.com/khuongnv/ping/releases/tag/v1.0.2`
