# 📋 Nhật Ký Thực Thi: Điều chỉnh tương tác thẻ Wi-Fi và vị trí nút Quét lại

- **Ngày thực hiện**: 2026-11-22
- **Agent phụ trách**: `compose-ui-specialist`, `lead-architect`, `qa-perf-specialist`
- **Yêu cầu gốc**: Hiện tại khi click vào tên WIFI thì đang chuyển sang Password Audit, tôi muốn chỉnh lại chỗ này, Click vào thì hiển thị đánh giá bảo mật, Dưới dòng đánh giá báo mật thì mới có dòng kiểm định và link sang Password audit. Chuyển nút Scan lại (Trên mục WIFI inspector) xuống chỗ PHÂN TÍCH TỔNG QUAN (Căn phải) để nó đúng ý nghĩa và dễ hiểu hơn.
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Kế Hoạch (Analysis & Planning)
- **Vấn đề nhận diện**:
  1. Thẻ `WifiNetworkCard` trước đây có sự kiện `clickable(onClick = onAuditClick)` gắn trên toàn bộ Card, khiến người dùng bấm vào tên Wi-Fi sẽ bị chuyển hướng đột ngột sang tab Password Auditor mà chưa kịp đọc thông tin bảo mật.
  2. Nút "Kiểm định mật khẩu mạng này" nằm cố định bên ngoài, gây chiếm diện tích và chưa hợp lý về mặt luồng trải nghiệm người dùng (UX flow).
  3. Nút Quét Wi-Fi (Scan / Refresh) nằm ở thanh tiêu đề trên cùng (Top Header) chưa trực quan; vị trí hợp lý nhất là đặt trong Card **PHÂN TÍCH TỔNG QUAN** và căn lề phải.
- **Mục tiêu kỹ thuật**:
  1. Đổi sự kiện click trên `WifiNetworkCard` thành đóng/mở chi tiết `isExpanded`. Bổ sung icon mũi tên thu gọn/mở rộng trực quan (`KeyboardArrowDown` / `KeyboardArrowUp`).
  2. Đưa nội dung "Đánh giá bảo mật: ..." cùng chuỗi mã hóa và nút "Kiểm định mật khẩu mạng này ->" vào bên trong `AnimatedVisibility(isExpanded)`: Dưới dòng đánh giá bảo mật mới xuất hiện dòng kiểm định và link sang Password Audit.
  3. Bỏ nút Scan khỏi top header ở tab 0, di chuyển xuống thẻ **PHÂN TÍCH TỔNG QUAN** căn phải với giao diện kính lỏng capsule sang trọng và trạng thái loading khi đang quét.

---

## 2. Chi Tiết Thực Hiện (Execution Steps)

### Bước 1: Điều chỉnh tương tác mở rộng `WifiNetworkCard` trong `ScanScreen.kt`
- Cập nhật `modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }` cho thẻ kính `LiquidGlassCard`.
- Bổ sung icon `Icons.Default.KeyboardArrowUp` và `Icons.Default.KeyboardArrowDown` cạnh badge bảo mật.
- Đặt dòng cảnh báo (nếu có) khi thu gọn và khi mở rộng.
- Đưa nút "Kiểm định mật khẩu mạng này" kèm icon và mũi tên chuyển hướng vào bên dưới dòng "Đánh giá bảo mật" bên trong `AnimatedVisibility`.

### Bước 2: Tái định vị nút Quét lại (Scan)
- Xóa nút Quét khỏi `Top Header` của `ScanScreen`.
- Chuyển nút Quét sang phần bên phải của thẻ **PHÂN TÍCH TỔNG QUAN** trong `NetworkScannerTab`:
  - Trạng thái bình thường: Nút kính lỏng viên thuốc có viền `PrimaryAccent` với icon Refresh và chữ "Quét lại".
  - Trạng thái đang quét: Hiển thị `CircularProgressIndicator` và chữ "Đang quét...".

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt` - Cập nhật tương tác click `WifiNetworkCard` mở rộng đánh giá bảo mật, nhúng nút liên kết kiểm định bên trong và đưa nút Quét lại xuống thẻ Phân tích tổng quan căn phải.
- `[Modified]` `docs/logs/README.md` - Cập nhật mục lục truy vết.

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- **Unit Tests**:
  - Chạy `./gradlew.bat test` ➜ `BUILD SUCCESSFUL in 30s` (Tất cả test case đều PASS).
- **Build APK**:
  - Chạy `./gradlew.bat assembleDebug` ➜ `BUILD SUCCESSFUL in 13s`.

---

## 5. Kết Luận
Giao diện đã đáp ứng trọn vẹn và hoàn hảo ý định của người dùng: Thao tác bấm vào thẻ Wi-Fi sẽ mở rộng phần đánh giá bảo mật một cách mượt mà và tự nhiên; chỉ khi mở rộng thì nút kiểm định mới xuất hiện ngay dưới phần đánh giá; nút Quét lại ở thẻ Phân tích tổng quan giúp bố cục cân đối và dễ hiểu hơn.
