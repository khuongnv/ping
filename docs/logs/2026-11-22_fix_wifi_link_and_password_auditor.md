# 📋 Nhật Ký Thực Thi: Sửa liên kết chọn mạng Wi-Fi sang Password Auditor & Sửa lỗi logic kiểm định từ điển

- **Ngày thực hiện**: 2026-11-22 15:49:48
- **Agent phụ trách**: `lead-architect`, `network-engine-specialist`, `compose-ui-specialist`, `qa-perf-specialist`
- **Yêu cầu gốc**: Sau khi quét mạng, click vào từng mạng thì có 1 liên kết để chuyển đúng mạng đó vào phần scan password. Phần scan mật khẩu cũng đang hoạt động không đúng, lúc nào cũng báo khớp 12345678. Sửa và hoàn thành thì release ver mới.
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Nguyên Nhân Lỗi (Root Cause Analysis)
1. **Lỗi liên kết chọn mạng Wi-Fi**:
   - Thẻ `WifiNetworkCard` trong `ScanScreen.kt` trước đây chỉ cho phép mở rộng thông tin khi bấm vào huy hiệu bảo mật, nút "Kiểm định" bị ẩn trong phần thu gọn và toàn bộ thẻ không có sự kiện click trực tiếp. Người dùng không thể chạm 1 chạm để chuyển mạng đã chọn sang Password Auditor.
2. **Lỗi logic Password Auditor luôn báo khớp `12345678`**:
   - Trong `WifiScanEngine.kt`, hàm `auditWifiPassword` có đoạn kiểm tra:
     ```kotlin
     val isMatched = if (!targetPasswordForSim.isNullOrBlank()) {
         candidate.equals(targetPasswordForSim, ignoreCase = false)
     } else {
         candidate == "12345678" || candidate == "admin123" || candidate == "wifi1234"
     }
     ```
   - Khi người dùng bấm "Bắt đầu kiểm định" mà chưa điền mật khẩu ở ô bên dưới, `targetPasswordForSim` là rỗng. Do từ điển bắt đầu bằng `12345678`, điều kiện `candidate == "12345678"` lập tức trả về `true` ngay ở lần thử #1 (`attemptNum = 1`), khiến chương trình dừng ngay lập tức và báo trúng khớp giả `12345678`.
   - Ngoài ra, ô nhập mật khẩu mục tiêu bị đặt riêng ở mục 3 phía dưới thay vì nằm ngay trong phần cấu hình kiểm định phía trên.

---

## 2. Giải Pháp & Chi Tiết Thực Hiện (Implementation Details)
1. **Sửa liên kết chọn mạng Wi-Fi (`ScanScreen.kt`)**:
   - Thêm sự kiện click toàn bộ thẻ `WifiNetworkCard` (`.clickable(onClick = onAuditClick)`).
   - Thêm nút liên kết trực quan nổi bật: `[🛡️ Kiểm định mật khẩu mạng này -> Audit]` ngay trên mỗi thẻ mạng.
   - Khi bấm, tự động:
     - Gán `config.targetSsid = item.ssid`.
     - Chuyển tab sang `selectedSubTab = 1` (Password Auditor).
     - Hiển thị Toast thông báo: `"Đã chuyển mạng \"$ssid\" sang Password Auditor"`.
2. **Sửa triệt để logic Password Auditor (`WifiScanEngine.kt`)**:
   - Xóa bỏ hoàn toàn điều kiện giả định sai `candidate == "12345678"`.
   - Phân biệt rõ 2 chế độ kiểm định:
     - **Chế độ 1 - Thẩm định mật khẩu mục tiêu cụ thể**: Người dùng nhập mật khẩu cần kiểm tra. Auditor duyệt qua tập từ điển; nếu tìm thấy candidate trùng khớp thì báo trúng khớp tại lần thử `#X`, cảnh báo nguy cơ từ điển; nếu không thấy thì báo an toàn trước từ điển thử nghiệm.
     - **Chế độ 2 - Quét kiểm định mẫu từ điển tổng quát**: Không có mật khẩu mục tiêu (`targetPassword = ""`). Auditor duyệt qua TOÀN BỘ tập mẫu theo bộ lọc (độ dài, loại ký tự), đo tốc độ thử nghiệm (`attempts/s`), cập nhật thanh tiến trình % và ghi nhận đầy đủ log terminal. Tuyệt đối không giả định trúng khớp bừa bãi.
3. **Tái cấu trúc giao diện Password Auditor (`ScanScreen.kt`)**:
   - Chuyển ô nhập mật khẩu mục tiêu và bảng thẩm định Entropy/thời gian bẻ khóa lên nằm trực tiếp bên trong thẻ `CẤU HÌNH PASSWORD AUDITOR` (ngay dưới ô SSID).
   - Loại bỏ thẻ mục 3 thừa, giúp luồng trải nghiệm người dùng liền mạch và trực quan.
4. **Kiểm thử tự động (`WifiScanEngineTest.kt`)**:
   - Bổ sung test case `testAuditWifiPassword_withoutTargetPassword_doesNotFakeMatch` xác minh khi không nhập mật khẩu thì duyệt hết từ điển và `matchedPassword` là `null`.
   - Bổ sung test case `testAuditWifiPassword_matchesSpecificPassword` xác minh tìm đúng `admin123` mà không bị ngắt ở `12345678`.
   - Chạy `./gradlew.bat test`: 100% PASS.

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/engine/WifiScanEngine.kt`: Sửa logic kiểm tra khớp trong `auditWifiPassword`, loại bỏ giả định `12345678`.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt`: Thêm nút liên kết 1 chạm chọn mạng Wi-Fi và tái cấu trúc giao diện Password Auditor.
- `[Modified]` `app/src/test/java/com/vnpt/pingtool/data/engine/WifiScanEngineTest.kt`: Bổ sung 2 bài unit test kiểm thử logic mới.
- `[Modified]` `docs/logs/README.md`: Cập nhật trạng thái chỉ mục nhật ký.

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- **Unit Tests**: Chạy `./gradlew.bat test` - 100% test suites (`WifiScanEngineTest`, `PingOutputParserTest`, `TelnetEngineTest`) PASS.
- **Biên dịch**: `./gradlew.bat compileDebugKotlin` - BUILD SUCCESSFUL.

---

## 5. Kết Luận & Hướng Dẫn Tiếp Theo (Conclusion & Next Steps)
- Lỗi liên kết chuyển mạng và lỗi báo khớp giả `12345678` đã được sửa hoàn toàn.
- Tiến hành kích hoạt quy trình phát hành phiên bản mới `v1.3.1` (Patch release) và gửi thông báo Telegram.
