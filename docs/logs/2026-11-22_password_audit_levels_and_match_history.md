# 📋 Nhật Ký Thực Thi: Tổ chức từ điển theo cấp độ 1-99, thanh kéo độ khó và màn hình xem log khớp mật khẩu

- **Ngày thực hiện**: 2026-11-22
- **Agent phụ trách**: `lead-architect`, `network-engine-specialist`, `compose-ui-specialist`, `qa-perf-specialist`
- **Yêu cầu gốc**: Tổ chức file json từ điển theo cấp độ khó: 1-> 99, Và trên app sẽ có 1 thanh để kéo độ khó này. Sẽ scan lần lượt theo cấp độ. Dừng khi khớp hoặc người dùng pause. Khi khớp thì ghi log. Có màn hình xem log này.
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Kế Hoạch (Analysis & Planning)
- **Mục tiêu kỹ thuật**:
  1. Phân loại và cấu trúc lại tập từ điển mật khẩu Wi-Fi phổ thông (1.684 mẫu) trong `wifi_passwords.json` theo thang đo cấp độ khó từ **1 đến 99**.
  2. Bổ sung thanh trượt Slider điều chỉnh độ khó tối đa (`1 -> 99`) trên giao diện Compose với nhãn phân cấp trực quan và phản hồi nhịp nhàng.
  3. Cải tiến thuật toán quét của `WifiScanEngine`: Lọc theo `level <= maxLevel` và sắp xếp quét tuần tự theo cấp độ tăng dần `compareBy({ it.level }, { it.password.length })`. Dừng ngay lập tức khi phát hiện khớp mật khẩu hoặc khi người dùng ấn Tạm dừng (Pause/Stop).
  4. Cơ chế lưu trữ bền vững (Persistent Storage): Tự động ghi lại bản ghi đối chiếu khớp (`PasswordAuditMatchRecord`) vào `PingStorage` qua SharedPreferences (JSON Gson).
  5. Xây dựng giao diện xem log khớp mật khẩu (`PasswordAuditHistoryDialog`) chuẩn phong cách Liquid Glass x Apple Music HIG: hiển thị SSID, mật khẩu khớp (với nút sao chép 1 chạm), huy hiệu cấp độ 1..99, đánh giá rủi ro, số lần thử, tốc độ quét và thời gian ghi nhận.

---

## 2. Chi Tiết Triển Khai (Execution Steps)

### Bước 1: Chuẩn hóa dữ liệu từ điển cấp độ 1-99
- Cập nhật cả 2 file `app/src/main/assets/wifi_passwords.json` và `docs/dictionary/wifi_passwords.json`:
  - Cấu trúc: `items: [{ "password": "...", "level": 1..99 }]`
  - Thang đo 1..99:
    - Cấp 1-15: Dãy số lặp hoặc dãy số thứ tự cực kỳ dễ đoán (`12345678`, `88888888`, `00000000`...).
    - Cấp 16-35: Năm sinh, ngày tháng, dãy 4 số lặp (`19901990`, `01011995`...).
    - Cấp 36-60: Từ khóa router & nhà mạng Việt Nam (`vnpt1234`, `viettel123`, `admin1234`...).
    - Cấp 61-85: Cụm từ thông dụng tiếng Việt và quốc tế (`khongcomatkhau`, `password123`...).
    - Cấp 86-99: Mật khẩu phức tạp kết hợp hoa, thường, số và ký tự đặc biệt (`Vnpt@123`, `Viettel@2024`...).

### Bước 2: Nâng cấp Data Models & Storage Layer
- `Models.kt`:
  - Bổ sung `DictionaryItem(val password: String, val level: Int = 1)`.
  - Bổ sung `val maxLevel: Int = 50` vào `PasswordAuditConfig`.
  - Bổ sung `val currentLevel: Int = 1`, `val matchedLevel: Int? = null` vào `PasswordAuditStatus`.
  - Tạo model `PasswordAuditMatchRecord` lưu thông tin đối chiếu khớp.
- `PingStorage.kt`:
  - Thêm `saveAuditMatches` và `loadAuditMatches` lưu tối đa 50 bản ghi gần nhất.

### Bước 3: Cải tiến Network Engine (`WifiScanEngine.kt`)
- `parseJsonDictionary`: Hỗ trợ phân tích cả định dạng `items` (có level) và fallback `passwords` (tự động tính level qua hàm `calculatePasswordLevel`).
- `auditWifiPassword`:
  - Lọc ứng viên: `item.level <= config.maxLevel` kết hợp bộ lọc độ dài và charset.
  - Sắp xếp tuần tự: `compareBy({ it.level }, { it.password.length })`.
  - Vòng lặp quét: Cập nhật `currentLevel`, dừng ngay lập tức khi phát hiện khớp, trả về `matchedLevel`.

### Bước 4: Mở rộng ViewModel (`PingViewModel.kt`)
- Bổ sung `auditHistory: StateFlow<List<PasswordAuditMatchRecord>>`.
- Nạp lịch sử đối chiếu khi khởi tạo ViewModel (`init`).
- Khi phát hiện khớp mật khẩu trong `startPasswordAudit()`, tự động đóng gói `PasswordAuditMatchRecord`, cập nhật StateFlow và lưu vào SharedPreferences.
- Bổ sung các phương thức: `clearAuditHistory()`, `deleteAuditHistoryRecord()`, `setAuditMaxLevel()`.

### Bước 5: Thiết kế giao diện Liquid Glass (`ScanScreen.kt` & `PasswordAuditHistoryDialog.kt`)
- Thêm thanh trượt Slider điều chỉnh độ khó từ điển từ 1 đến 99, kèm nhãn phân cấp màu động (Rất dễ, Dễ, Trung bình, Khó, Rất khó).
- Thêm thẻ banner "Lịch sử mật khẩu đã khớp" hiển thị số lượng bản ghi kèm lối tắt mở hộp thoại xem log.
- Tạo `PasswordAuditHistoryDialog.kt`: Giao diện Liquid Glass hiển thị danh sách các mạng đã phát hiện mật khẩu khớp, cho phép sao chép mật khẩu 1 chạm hoặc xóa từng bản ghi/xóa toàn bộ.

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/src/main/assets/wifi_passwords.json` - Từ điển 1.684 mật khẩu phân loại cấp 1-99.
- `[Modified]` `docs/dictionary/wifi_passwords.json` - Bản lưu từ điển đồng bộ trên kho mã nguồn.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/model/Models.kt` - Thêm `DictionaryItem`, `maxLevel`, `currentLevel`, `matchedLevel`, `PasswordAuditMatchRecord`.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/storage/PingStorage.kt` - Thêm logic lưu/đọc `PasswordAuditMatchRecord`.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/engine/WifiScanEngine.kt` - Xử lý lọc, quét theo thứ tự level 1..99 và trả về kết quả khớp.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/viewmodel/PingViewModel.kt` - Quản lý `auditHistory`, tự động lưu khi khớp, clear/delete bản ghi.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt` - Bổ sung Slider độ khó 1..99, thẻ xem lịch sử, kết nối modal dialog.
- `[Created]` `app/src/main/java/com/vnpt/pingtool/ui/components/PasswordAuditHistoryDialog.kt` - Hộp thoại modal xem log mật khẩu đã khớp.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/MainActivity.kt` - Kết nối `auditHistory` giữa ViewModel và `ScanScreen`.
- `[Modified]` `app/src/test/java/com/vnpt/pingtool/data/engine/WifiScanEngineTest.kt` - Bổ sung bài kiểm thử cho `calculatePasswordLevel` và lọc `maxLevel`.

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- **Unit Tests**:
  - Chạy `./gradlew.bat test`: Toàn bộ các bài kiểm thử trong `WifiScanEngineTest`, `PingEngineTest`, `TelnetEngineTest` đều đạt `BUILD SUCCESSFUL` (0 lỗi).
- **Build APK**:
  - Chạy `./gradlew.bat assembleDebug`: `BUILD SUCCESSFUL in 25s`, tạo file APK hoàn chỉnh không cảnh báo nghiêm trọng.

---

## 5. Kết Luận
Tính năng tổ chức từ điển theo cấp độ 1-99, thanh trượt kéo độ khó trên giao diện và màn hình xem log đối chiếu mật khẩu đã hoàn thành xuất sắc, tuân thủ nghiêm ngặt chuẩn kiến trúc MVVM, giao diện Liquid Glass x Apple Music HIG và cam kết 100% Zero Ads.
