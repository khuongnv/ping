# 📋 Nhật Ký Thực Thi: Tổ chức Sidebar thành Menu, thêm Tab Scan Wi-Fi Inspector & Password Auditor

- **Ngày thực hiện**: 2026-11-22 15:28:56
- **Agent phụ trách**: `lead-architect`, `compose-ui-specialist`, `device-network-specialist`, `qa-perf-specialist`
- **Yêu cầu gốc**: Tổ chức sidebar (nav drawer) thành dạng menu (Cài đặt, IP của tôi đưa vào sidebar, bỏ khỏi taskbar). Thêm vào taskbar tab tính năng mới: Scan (Wi-Fi Inspector: Quét Wi-Fi lân cận, phân tích bảo mật; Password Auditor: kiểm định độ dài, chỉ số hay cả chữ, từ điển mật khẩu mạng, đếm số lần thử và terminal log).
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Kế Hoạch (Analysis & Planning)
- **Bối cảnh & Mục tiêu kỹ thuật**: 
  - Tinh gọn thanh điều hướng đáy (Bottom Bar) về 3 công cụ mạng cốt lõi: **Ping**, **Telnet**, và **Scan** (Wi-Fi Inspector).
  - Tái cấu trúc thanh trượt điều hướng Sidebar (`AppSidebar.kt`) thành Menu chuyên nghiệp chuẩn Apple Music HIG x Liquid Glass với các danh mục:
    - 🌐 **IP của tôi**: Thông tin mạng nội bộ, Wi-Fi hiện tại, Public IP & ISP.
    - ⚙️ **Cài đặt**: Tùy chỉnh tham số gói tin Ping (kích thước, interval, timeout, số lượng gói).
    - 🚀 **Cập nhật phần mềm (OTA)**: Kiểm tra và tải bản phát hành mới từ GitHub Releases.
    - ℹ️ **Thông tin dự án**: Cam kết Zero Ads, liên kết mã nguồn GitHub.
  - Xây dựng module **Wi-Fi Inspector**:
    - **Network Scanner**: Quét danh sách Access Points thực tế qua `WifiManager`, trích xuất SSID, BSSID, RSSI dBm, số vạch sóng, tần số MHz & Kênh phát sóng (2.4GHz / 5GHz / 6GHz).
    - **Security Analyzer**: Phân loại WPA3-Personal (SAE), WPA2-PSK (AES/CCMP), WPA-PSK (TKIP), WEP, Enterprise 802.1X, và Mạng mở (Open). Cảnh báo nghiêm trọng đối với mạng không mã hóa (nguy cơ bắt gói Sniffing, Man-in-the-Middle) và WEP lỗi thời.
    - **Password Auditor**:
      - Cho phép người dùng tùy chọn cấu hình: Độ dài (Min - Max), Dạng ký tự (Chỉ số 0-9, Chữ & Số, Kèm ký tự đặc biệt, Tất cả từ điển).
      - Tích hợp sẵn offline từ điển 500+ mật khẩu phổ biến nhất thế giới và Việt Nam (dãy số lặp, năm sinh, chuỗi mặc định của router VNPT, Viettel, FPT, v.v.).
      - Nút đồng bộ tải từ điển mới nhất từ mạng (Online Sync qua GitHub SecLists).
      - Đếm số lần thử (`current / total`), tính toán tốc độ thử thực tế (`attempts/s`), thanh tiến trình mượt mà.
      - Phân tích Shannon Entropy và ước tính thời gian bẻ khóa brute-force dựa trên GPU/CPU.
      - Terminal Log Panel hiển thị log trực tiếp kiểu hacker theo thời gian thực (đầy đủ timestamp `HH:mm:ss.SSS`, mức độ log INFO / WARN / ALERT / SUCCESS).

---

## 2. Chi Tiết Các Bước Thực Hiện (Execution Steps)
1. **Khởi tạo log truy vết & thông báo Telegram**:
   - Gọi script `init_log.ps1` tạo `docs/logs/2026-11-22_wifi_inspector_scan_sidebar.md`.
   - Gửi điện văn `TASK_START` và `PLAN_FINISH` đến kênh Telegram `HNiBOT`.
2. **Mở rộng mô hình dữ liệu (`data/model/Models.kt`)**:
   - Thêm `WifiSecurityType`, `WifiScanItem`, `PasswordAuditCharset`, `PasswordAuditConfig`, `PasswordAuditStatus`, `AuditLogLevel`, `AuditLogEntry`, `PasswordStrengthResult`.
3. **Phát triển động cơ Wi-Fi Inspector & Password Auditor (`data/engine/WifiScanEngine.kt`)**:
   - Xử lý quyền vị trí và thiết bị lân cận (`ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES`).
   - Phân tích chuỗi capabilities để nhận diện WPA3, WPA2, WPA, WEP, OPEN, ENTERPRISE.
   - Hàm `evaluatePasswordStrength`: Tính toán độ dài, tập ký tự, tra cứu thứ hạng trong từ điển thông dụng, tính Shannon Entropy và đưa ra khuyến nghị cải thiện.
   - Hàm `auditWifiPassword`: Chạy Flow bất đồng bộ giả lập kiểm định từ điển theo bộ lọc độ dài và loại ký tự, đếm số lần thử và stream các dòng nhật ký.
   - Hàm `syncOnlineDictionary`: Tải danh sách mật khẩu phổ biến từ mạng với cơ chế fallback an toàn.
4. **Tái thiết kế `AppSidebar.kt` thành Navigation Menu**:
   - Chuyển thành Menu Drawer dạng danh mục với các thẻ item kính mờ Liquid Glass: `IP của tôi`, `Cài đặt`, `Cập nhật OTA`, `Về ứng dụng`.
   - Thao tác chọn item tự động đóng sidebar và chuyển hướng điều hướng màn hình.
5. **Cập nhật `FloatingGlassBottomBar.kt` & `MainActivity.kt`**:
   - Đưa `my_ip` ra khỏi thanh điều hướng đáy; cấu hình 3 tab đáy: `Ping`, `Telnet`, `Scan`.
   - Bổ sung màn hình `SettingsScreen` và `MyIpScreen` vào điều hướng từ sidebar với nút quay lại (Back arrow).
6. **Xây dựng giao diện `ScanScreen.kt`**:
   - Phân chia 2 chế độ xem: `Quét mạng & Bảo mật` (Network Scanner) và `Password Auditor`.
   - Tích hợp bảng thống kê trực quan, thẻ Access Point với biểu tượng sóng, cảnh báo rủi ro bảo mật mạng mở, bảng điều khiển thông số Password Auditor và Terminal Log Panel.
7. **Kiểm thử tự động & Xác minh chất lượng**:
   - Viết trọn bộ 8 bài test chuyên sâu trong `WifiScanEngineTest.kt`.
   - Chạy kiểm thử `./gradlew.bat test` và đóng gói thử `./gradlew.bat assembleDebug`: 100% PASS không có lỗi.

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Created]` `app/src/main/java/com/vnpt/pingtool/data/engine/WifiScanEngine.kt`: Động cơ quét Wi-Fi, phân tích mã hóa, kiểm định mật khẩu và từ điển.
- `[Created]` `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt`: Màn hình giao diện Liquid Glass cho Wi-Fi Inspector và Password Auditor.
- `[Created]` `app/src/test/java/com/vnpt/pingtool/data/engine/WifiScanEngineTest.kt`: Unit tests kiểm thử phân loại bảo mật, entropy và mô phỏng quét từ điển.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/model/Models.kt`: Bổ sung data class và enum cho Wi-Fi scan, bảo mật và password auditor.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/components/AppSidebar.kt`: Chuyển đổi thành menu điều hướng drawer với các mục IP của tôi và Cài đặt.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/SettingsScreen.kt`: Thêm hỗ trợ nút Back arrow và mở drawer.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/MyIpScreen.kt`: Thêm hỗ trợ nút Back arrow khi được mở từ Sidebar.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/viewmodel/PingViewModel.kt`: Tích hợp các StateFlow và phương thức điều phối Wi-Fi Scan và Password Auditor.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/MainActivity.kt`: Cập nhật 3 tab chính đáy (Ping, Telnet, Scan) và kết nối các màn hình mới.

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- **Unit Tests**: Chạy `./gradlew.bat test` - Toàn bộ các test suite (`WifiScanEngineTest`, `PingOutputParserTest`, `TelnetEngineTest`) đều chạy thành công (`BUILD SUCCESSFUL`).
- **Biên dịch APK**: Chạy `./gradlew.bat assembleDebug` thành công trong 55s, không có cảnh báo nghiêm trọng.
- **Tuân thủ quy chuẩn Zero Ads & Liquid Glass**: 100% không quảng cáo, giao diện AMOLED Black `#000000`, viền phản quang `0.75dp Luminous Rim`, hoạt ảnh Spring physics đàn hồi tự nhiên.

---

## 5. Kết Luận & Hướng Dẫn Tiếp Theo (Conclusion & Next Steps)
- Tính năng Wi-Fi Inspector, Security Analyzer, Password Auditor và cấu trúc Sidebar Menu đã hoàn thành trọn vẹn theo đúng yêu cầu người dùng và ảnh thiết kế mẫu.
- Sẵn sàng đồng bộ mã nguồn lên Git và gửi báo cáo hoàn thành đến Telegram.
