# Kế Hoạch Triển Khai Kiến Trúc: VNPT Router Controller (Thay Thế Module Scan)

- **Mã kế hoạch:** `PLAN_20261126_01_ARCHITECTURE`
- **Mục tiêu:** Xóa bỏ hoàn toàn phân hệ `Scan Wi-Fi & Password Audit` (không còn hữu ích); thay thế bằng hệ thống **VNPT Router Controller & Access Manager** tương tác trực tiếp với Modem chính (VNPT iGate GW040H) và Bộ phát Wi-Fi phụ (AP / Mesh).
- **Trạng thái:** Chờ phê duyệt (Pending Approval)
- **Quy chuẩn tuân thủ:** Clean Architecture, Coroutine StateFlow, Android Jetpack Security, WorkManager.

---

## 1. Dọn Dẹp Phân Hệ Cũ (Deprecation & Removal)

Để giữ mã nguồn sạch sẽ, không rác và không phình to kích thước APK, các thành phần sau sẽ được loại bỏ:
* **Data Engines:**
  * Xóa bỏ hoặc hủy kích hoạt `data/engine/WifiScanEngine.kt` (quét danh sách SSID, tần số, mật khẩu).
  * Xóa bỏ hoặc dọn dẹp `data/network/WifiConnectionManager.kt` (thử nghiệm kết nối WPA2/WPA3 qua NetworkSpecifier).
* **Models & Storage liên quan:**
  * Xóa các data models: `WifiScanItem`, `WifiAuditConfig`, `AuditStatus`, `AuditHistoryRecord`, `AuditDictionaryEntry` khỏi `data/model/Models.kt`.
  * Xóa các hàm lưu trữ nhật ký audit và từ điển mật khẩu trong `data/storage/PingStorage.kt`.
* **ViewModel & MainActivity:**
  * Loại bỏ các StateFlow liên quan đến audit (`wifiScanList`, `isWifiScanning`, `auditLogs`, `auditStatus`, `auditConfig`, `dictionaryCount`, v.v.).

---

## 2. Thiết Kế Phân Hệ Mới: VNPT Router Controller Data Layer

```text
com.vnpt.pingtool.data/
├── engine/
│   ├── router/
│   │   ├── VnptRouterEngine.kt        # Quản lý vòng đời kết nối Telnet Socket (Transient session)
│   │   ├── RouterCommandParser.kt     # Phân tích cú pháp chuỗi từ /proc/net/arp, udhcpd, iptables, tcapi
│   │   └── scheduler/
│   │       ├── RouterScheduleWorker.kt# Android WorkManager chạy nền kích hoạt rule theo lịch
│   │       └── ScheduleManager.kt     # Đăng ký & quản lý các công việc WorkManager
├── model/
│   ├── router/
│   │   ├── RouterDevice.kt            # IP, MAC, Hostname, Custom Alias, Vendor, isBlocked, isPriority
│   │   ├── PonOpticalStats.kt         # RxPower (dBm), TxPower (dBm), Temperature (°C)
│   │   ├── ApNodeInfo.kt              # Tên AP phụ, IP, MAC, WebUrl, Ghi chú
│   │   ├── ScheduleRule.kt            # RuleId, DeviceMac, StartTime, EndTime, DaysOfWeek, IsActive
│   │   └── RouterCredentials.kt       # GatewayIp, Port, Username, Password (Encrypted)
└── storage/
    └── RouterStorage.kt               # Lưu cài đặt kết nối, Alias tên thiết bị, Danh sách VIP, AP phụ, Lịch
```

---

## 3. Chi Tiết Kỹ Thuật Từng Thành Phần

### 3.1. `VnptRouterEngine.kt` (Engine tương tác Telnet)
* **Nguyên lý kết nối ngắn hạn (Transient Session with Mutex):**
  * Daemon Telnet trên modem iGate GW040H yếu, dễ crash nếu duy trì socket dài hạn.
  * Engine áp dụng mẫu thiết kế: **Mở Socket -> Xác thực Login/Pass -> Gửi lệnh -> Đọc phản hồi tới dấu nhắc `#` -> Gửi `exit` -> Đóng Socket**.
  * Sử dụng `kotlinx.coroutines.sync.Mutex` để đảm bảo tại một thời điểm chỉ có 1 tác vụ Telnet được thực thi, chống tràn session.
* **Tập lệnh cốt lõi:**
  1. `fetchActiveDevices()`:
     - Gửi `cat /proc/net/arp` -> phân tích IP, MAC.
     - Gửi `cat /etc/udhcpd.leases 2>/dev/null || cat /tmp/udhcpd.leases` -> lấy Hostname DHCP.
     - Gửi `iptables -L FORWARD -n` -> đối soát xem MAC nào đang có rule `DROP`.
     - Ghép nối dữ liệu trả về `List<RouterDevice>`.
  2. `blockDevice(mac: String)`:
     - Gửi: `iptables -D FORWARD -m mac --mac-source <mac> -j DROP 2>/dev/null`
     - Gửi: `iptables -I FORWARD -m mac --mac-source <mac> -j DROP`
     - Kiểm tra kết quả phản hồi.
  3. `unblockDevice(mac: String)`:
     - Gửi: `iptables -D FORWARD -m mac --mac-source <mac> -j DROP`
  4. `fetchOpticalHealth()`:
     - Gửi: `tcapi show Info_PonPhy`
     - Trả về `PonOpticalStats(rxPower, txPower, temperature)`.
  5. `testConnection(ip, port, user, pass)`:
     - Trả về `Result<Boolean>` (Thành công hoặc lý do lỗi: Sai pass, Connection refused, Timeout).

### 3.2. `RouterCommandParser.kt` (Bộ phân tích cú pháp)
* **Parse ARP Table (`/proc/net/arp`):**
  - Tách theo khoảng trắng liên tiếp `\s+`.
  - Điều kiện: Cột `Flags` (`0x2` = hoàn tất phân giải) và cột `Device` (`br0`).
  - Lấy `IP` (cột 0) và `MAC` (cột 3, chuẩn hóa chữ hoa).
* **Parse DHCP Leases (`udhcpd.leases`):**
  - Trích xuất MAC và Hostname của client để gán tên máy ban đầu (VD: `iPhone-14-Pro`, `Galaxy-S23`).
* **Parse PON Info (`tcapi show Info_PonPhy`):**
  - Regex tìm `RxPower: ([-0-9\.]+)`, `TxPower: ([-0-9\.]+)`, `Temperature: ([0-9\.]+)`.

### 3.3. `RouterStorage.kt` (Lưu trữ an toàn)
* **Bảo mật Credentials:** Sử dụng `EncryptedSharedPreferences` (thư viện `androidx.security:security-crypto`) để lưu mật khẩu quản trị modem.
* **Lưu trữ Cấu hình & Trạng thái:**
  - `device_aliases`: Map JSON `MAC -> Tên gợi nhớ` (VD: `34:AB:... -> "Máy con gái"`).
  - `priority_macs`: Set các MAC nằm trong danh sách ưu tiên (VIP).
  - `ap_nodes`: Danh sách các bộ phát Wi-Fi phụ (IP, Tên, MAC).
  - `schedules`: Danh sách lịch chặn theo giờ `List<ScheduleRule>`.

### 3.4. `RouterScheduleWorker.kt` & `ScheduleManager.kt` (Lập lịch chặn theo giờ)
* **Cơ chế:**
  - Sử dụng **Android WorkManager** (`PeriodicWorkRequest` hoặc `OneTimeWorkRequest` lặp lại) chạy ngầm ngay cả khi app không mở.
  - Mỗi khi đến mốc giờ bắt đầu chặn: Worker kiểm tra nếu đang kết nối Wi-Fi cùng dải IP modem -> Kết nối Telnet và thực thi lệnh chặn.
  - Khi đến mốc giờ mở: Worker kết nối Telnet và xóa rule `iptables DROP`.
  - Có cơ chế retry (BackoffCriteria) nếu lúc đó thiết bị đang tạm thời mất kết nối Wi-Fi nhà.

---

## 4. Tác Động Mã Nguồn Dự Kiến (File Impact List)

1. `app/build.gradle.kts`: Thêm dependency `androidx.work:work-runtime-ktx` và `androidx.security:security-crypto` (nếu chưa có).
2. `app/src/main/java/com/vnpt/pingtool/data/engine/router/VnptRouterEngine.kt` (Tạo mới)
3. `app/src/main/java/com/vnpt/pingtool/data/engine/router/RouterCommandParser.kt` (Tạo mới)
4. `app/src/main/java/com/vnpt/pingtool/data/engine/router/scheduler/RouterScheduleWorker.kt` (Tạo mới)
5. `app/src/main/java/com/vnpt/pingtool/data/engine/router/scheduler/ScheduleManager.kt` (Tạo mới)
6. `app/src/main/java/com/vnpt/pingtool/data/model/RouterModels.kt` (Tạo mới)
7. `app/src/main/java/com/vnpt/pingtool/data/storage/RouterStorage.kt` (Tạo mới)
8. `app/src/main/java/com/vnpt/pingtool/ui/viewmodel/PingViewModel.kt`: Tích hợp các StateFlow quản lý Router và xóa bỏ các StateFlow cũ của Wifi Audit.
