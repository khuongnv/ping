# VNPT Router & AP Controller — Đặc Tả Hệ Thống Giám Sát, Quản Lý & Điều Khiển Mạng Nội Bộ

> **Định vị trong VNPT Ping Tool:** Tính năng này thay thế hoàn toàn cho module *Scan Wi-Fi & Password Audit* trước đây. Module mới đóng vai trò là **Trung Tâm Giám Sát & Điều Khiển Router (Router Controller & Network Access Manager)**, trực tiếp kết nối với Modem chính (VNPT iGate GW040H) và các Bộ phát Wi-Fi phụ (AP / Mesh) để quản lý thiết bị, chặn/mở mạng tức thì, thiết lập danh sách ưu tiên và quản lý lịch chặn truy cập theo giờ.

---

## 1. Tổng Quan Kiến Trúc & Luồng Mạng

Hệ thống hoạt động theo mô hình mạng gia đình / doanh nghiệp vừa và nhỏ phổ biến của VNPT:
1. **Modem Router chính (Gateway):** VNPT iGate GW040H (hoặc các dòng iGate tương đương) đóng vai trò Gateway quay PPPoE, cấp phát DHCP, định tuyến NAT, Firewall `iptables` và kết nối cáp quang GPON.
2. **Bộ phát Wi-Fi phụ (AP / Mesh):** Ví dụ Xiaomi AX1800, Deco, Huawei, TP-Link... kết nối với GW040H qua cáp LAN ở chế độ **Access Point (AP Mode / Bridge)**. Mọi thiết bị kết nối vào AP phụ vẫn nhận dải IP `192.168.1.x` do modem chính cấp và giữ nguyên địa chỉ MAC gốc ở tầng Layer 2.
3. **Ứng dụng di động (VNPT Ping Tool):** Kết nối qua Wi-Fi nội bộ, mở luồng TCP Socket trực tiếp (Telnet/API) tới Gateway `192.168.1.1` để thực thi lệnh quản trị, đồng thời truy vấn thông tin AP phụ và các thiết bị đang online.

```text
               ┌────────────────────────────────────────────────────────┐
               │             Internet / GPON Optical Fiber              │
               └───────────────────────────┬────────────────────────────┘
                                           │ (Cáp quang GPON)
                                           ▼
               ┌────────────────────────────────────────────────────────┐
               │         MODEM ROUTER CHÍNH: VNPT iGate GW040H          │
               │  • IP: 192.168.1.1 (Cấu hình tùy biến)                 │
               │  • Vai trò: DHCP Server, NAT, iptables Firewall        │
               │  • Giao thức quản trị: Telnet (Port 23)                │
               │  • Thông số GPON: Rx/Tx Optical Power (dBm)            │
               └───────────┬────────────────────────────────┬───────────┘
                           │ (LAN 1Gbps / Bridge br0)       │ (Wi-Fi 2.4/5GHz chính)
                           ▼                                ▼
       ┌──────────────────────────────────────┐     ┌───────────────────────┐
       │     BỘ PHÁT WI-FI PHỤ (AP / MESH)    │     │  Mobile App (Android) │
       │  • VD: Xiaomi AX1800, Deco, TP-Link  │     │  • VNPT Ping Tool     │
       │  • Chế độ: AP Mode (Bridge / Layer 2)│     │  • Quản trị & Giám sát│
       │  • IP quản trị AP: 192.168.1.x       │     └───────────────────────┘
       └───────────────────┬──────────────────┘
                           │ (Wi-Fi phụ)
                           ▼
              ┌───────────────────────────┐
              │     THIẾT BỊ ĐẦU CUỐI     │
              │  • Điện thoại, Laptop, TV │
              │  • Camera, Smart Home...  │
              │  (Giữ nguyên MAC thật)    │
              └───────────────────────────┘
```

---

## 2. Các Phân Hệ Tính Năng Chi Tiết

### 2.1. Quản Trị Modem Router Chính (VNPT Gateway)
* **Cấu hình thông tin đăng nhập:**
  * Địa chỉ IP Gateway: Mặc định `192.168.1.1` (tự động nhận diện từ Default Gateway của kết nối Wi-Fi hiện tại, cho phép người dùng sửa đổi).
  * Cổng Telnet: Mặc định `23`.
  * Tài khoản quản trị (`Username`): Mặc định `admin` hoặc `operator`.
  * Mật khẩu quản trị (`Password`): Nhập từ tem đáy modem hoặc mật khẩu người dùng đã đổi (được mã hóa lưu an toàn trên máy).
* **Kiểm tra kết nối (Test Connection):** Kiểm tra khả năng mở Socket và đăng nhập thành công vào shell BusyBox.
* **Chẩn đoán sức khỏe cáp quang (PON Optical Diagnostics):**
  * Đọc lệnh `tcapi show Info_PonPhy` trên GW040H.
  * Hiển thị:
    * **Rx Power (Công suất thu quang):** Đánh giá chất lượng đường truyền (🟢 Tốt: `-15` đến `-24 dBm`, 🟡 Suy hao nhẹ: `-25` đến `-27 dBm`, 🔴 Nguy cơ rớt mạng: `< -27 dBm`).
    * **Tx Power (Công suất phát quang):** Thông số phát quang từ modem lên OLT.
    * **PON Chip Temperature:** Nhiệt độ chip quang của modem.

### 2.2. Quản Trị Thông Tin Bộ Phát Wi-Fi Phụ (Access Point / Mesh)
* Hỗ trợ lưu trữ thông tin nhận diện bộ phát phụ trong nhà (như Xiaomi AX1800):
  * Tên bộ phát (ví dụ: *Xiaomi AX1800 Tầng 2*, *Deco Phòng Ngủ*).
  * Địa chỉ IP của bộ phát phụ (ví dụ: `192.168.1.254`).
  * Địa chỉ MAC của cổng LAN/WAN bộ phát phụ.
  * Đường dẫn truy cập nhanh Web Admin của AP phụ (HTTP/HTTPS).
  * Nhận diện và gán nhãn các thiết bị đang kết nối qua AP phụ dựa trên BSSID/ARP.

### 2.3. Quản Lý Toàn Bộ Thiết Bị Kết Nối (Connected Devices Dashboard)
* **Quét thiết bị online đa tầng:**
  * Đọc bảng ARP từ modem chính: `cat /proc/net/arp` (lọc giao diện `br0`, trạng thái `0x2`).
  * Đọc bảng cấp phát DHCP Lease: `cat /etc/udhcpd.leases` để lấy Hostname gốc của máy.
* **Thông tin hiển thị từng thiết bị:**
  * Địa chỉ IP nội bộ (`192.168.1.x`).
  * Địa chỉ MAC vật lý (`XX:XX:XX:XX:XX:XX`).
  * Tên gợi nhớ người dùng đặt (Custom Alias: ví dụ *iPhone của Bin*, *Tivi Samsung Phòng Khách*).
  * Hostname do DHCP cấp (ví dụ: `android-1a2b3c`, `DESKTOP-ABC123`).
  * Nhà sản xuất Card mạng (OUI Vendor Lookup từ MAC, ví dụ: Apple, Samsung, Sony, Xiaomi...).
  * Trạng thái kết nối (Online / Vắng mặt).
  * Nhãn phân loại: Thiết bị ưu tiên (VIP), Bị chặn (Blocked), hoặc Bình thường (Normal).

### 2.4. Tính Năng Chặn & Mở Mạng Tức Thì (Instant Block & Unblock)
* **Cơ chế can thiệp tầng hạt nhân (Kernel-level Firewall):**
  * Tác động trực tiếp vào chuỗi `FORWARD` của Linux `iptables` trên GW040H.
  * **Lệnh Chặn (Block):**
    ```bash
    iptables -D FORWARD -m mac --mac-source <MAC> -j DROP 2>/dev/null
    iptables -I FORWARD -m mac --mac-source <MAC> -j DROP
    ```
    *Ý nghĩa:* Ngắt ngay lập tức mọi gói tin chuyển tiếp ra Internet của địa chỉ MAC này trong < 0.2 giây. Thiết bị vẫn giữ kết nối Wi-Fi, vẫn có IP nhưng không thể vào mạng.
  * **Lệnh Mở (Unblock):**
    ```bash
    iptables -D FORWARD -m mac --mac-source <MAC> -j DROP
    ```
* **Tab Danh Sách Đang Bị Chặn (Blocked Devices Tab):**
  * Liệt kê tập trung tất cả thiết bị đang bị DROP gói tin (đọc từ `iptables -L FORWARD -n`).
  * Nút "Mở mạng toàn bộ (Unblock All)" với 1 chạm.

### 2.5. Danh Sách Ưu Tiên (Priority / VIP Whitelist)
* Người dùng có thể đánh dấu các thiết bị thiết yếu (điện thoại của cha mẹ, máy làm việc, camera an ninh, thiết bị nhà thông minh) vào **Danh Sách Ưu Tiên**.
* **Lợi ích an toàn:**
  * **Khóa bảo vệ chống chặn nhầm (Accidental Block Protection):** Khi thao tác "Chặn tất cả" hoặc thao tác nhanh, hệ thống tự động loại trừ các thiết bị nằm trong Danh Sách Ưu Tiên.
  * Biểu tượng vương miện/ngôi sao vàng đặc trưng trên giao diện Liquid Glass.
  * Cảnh báo xác nhận 2 bước nếu người dùng cố tình muốn chặn một thiết bị VIP.

### 2.6. Danh Sách Chặn Theo Giờ (Schedule-based Blocking / Giờ Giới Nghiêm)
* Phục vụ nhu cầu quản lý con cái học bài và ngủ đúng giờ (Parental Control Schedule).
* **Cấu hình lịch:**
  * Chọn thiết bị mục tiêu (hoặc nhóm thiết bị).
  * Chọn khung giờ chặn (ví dụ: *22:00 - 06:00* hàng ngày, hoặc *19:00 - 21:00* các ngày trong tuần Thứ 2 - Thứ 6).
  * Tên lịch gợi nhớ (ví dụ: *Giờ ngủ của con*, *Giờ tập trung học tập*).
* **Cơ chế thực thi trên Android:**
  * Sử dụng **Android WorkManager** lập lịch ngầm định kỳ hoặc hẹn giờ chính xác (`PeriodicWorkRequest` / `OneTimeWorkRequest`).
  * Khi đến giờ bắt đầu: App tự động kết nối Telnet tới GW040H và đẩy lệnh `iptables -I FORWARD ... -j DROP`.
  * Khi đến giờ kết thúc: App tự động kết nối Telnet và xóa rule `iptables -D FORWARD ... -j DROP`.
  * Có nhật ký kích hoạt lịch (Schedule Execution Logs) hiển thị trạng thái thực thi thành công hay thất bại (do mất kết nối Wi-Fi lúc đó).

---

## 3. Đặc Tả Giao Thức Telnet Session & Tập Lệnh GW040H

Mọi giao tiếp đều chạy trên `Dispatchers.IO` thông qua **TelnetClient Session** có hàng đợi (Mutex) tránh xung đột:

| Nhóm chức năng | Lệnh Shell thực thi | Ý nghĩa & Xử lý phản hồi (Parser) |
| :--- | :--- | :--- |
| **Bắt tay & Đăng nhập** | `admin`<br>`<password>` | Lắng nghe luồng byte đến khi gặp `Login:` -> gửi user; gặp `Password:` -> gửi pass; gặp `#` -> sẵn sàng nhận lệnh. |
| **Quét ARP Table** | `cat /proc/net/arp` | Tách các dòng có `Flags == 0x2` và `Device == br0`. Cột 1 = IP, cột 4 = MAC. |
| **Quét DHCP Hostname** | `cat /etc/udhcpd.leases` hoặc `cat /tmp/udhcpd.leases` | Phân tích cặp `MAC Address` <-> `Hostname` do thiết bị khai báo khi xin IP. |
| **Kiểm tra trạng thái Chặn** | `iptables -L FORWARD -n` | Tìm dòng chứa `DROP` và `--mac-source <MAC>`. Trích xuất các MAC đang bị chặn. |
| **Chặn Internet** | `iptables -D FORWARD -m mac --mac-source <MAC> -j DROP 2>/dev/null`<br>`iptables -I FORWARD -m mac --mac-source <MAC> -j DROP` | Chèn rule DROP vào đầu chuỗi FORWARD. |
| **Mở Internet** | `iptables -D FORWARD -m mac --mac-source <MAC> -j DROP` | Xóa rule DROP khỏi chuỗi FORWARD. |
| **Xem công suất quang** | `tcapi show Info_PonPhy` | Parse `RxPower`, `TxPower`, `Temperature`. |
| **Đóng phiên** | `exit` | Đóng kết nối Socket ngay sau khi xong tác vụ để giải phóng RAM cho modem. |

---

## 4. Kiến Trúc Mã Nguồn Đề Xuất (Thay Thế Module Scan)

```text
app/src/main/java/com/vnpt/pingtool/
├── data/
│   ├── engine/
│   │   ├── VnptRouterEngine.kt          # Điều phối kết nối Socket Telnet, session mutex, tập lệnh
│   │   ├── RouterCommandParser.kt       # Parser output ARP, udhcpd, iptables, PonPhy
│   │   └── scheduler/
│   │       ├── RouterScheduleWorker.kt  # WorkManager thực thi chặn/mở theo lịch
│   │       └── ScheduleManager.kt       # Đăng ký Alarm / WorkManager lịch chặn
│   ├── model/
│   │   ├── RouterModels.kt              # DeviceItem, PonInfo, BlockRule, ScheduleRule, ApNode
│   │   └── RouterCredentials.kt         # IP, Port, Username, Encrypted Password
│   └── storage/
│       └── RouterStorage.kt             # Room / SharedPreferences lưu Alias, VIP list, Schedules, AP list
├── ui/
│   ├── screens/
│   │   └── RouterManagerScreen.kt       # Màn hình chính thay thế ScanScreen (Tab 3)
│   ├── components/
│   │   ├── RouterDeviceItemCard.kt      # Thẻ thiết bị Liquid Glass + Switch Chặn/Mở + Menu VIP
│   │   ├── OpticalHealthCard.kt         # Thẻ công suất quang Rx/Tx cao cấp
│   │   ├── ApNodeInfoCard.kt            # Thẻ thông tin bộ phát Wi-Fi phụ
│   │   ├── ScheduleRuleDialog.kt        # Dialog tạo & sửa lịch chặn theo giờ
│   │   └── RouterConfigDialog.kt        # Dialog nhập IP, Port, User, Pass modem
│   └── viewmodel/
│       └── RouterViewModel.kt           # StateFlow quản lý danh sách thiết bị, quy tắc, log
```

---

## 5. Lưu Ý Kỹ Thuật Quan Trọng Khi Triển Khai

1. **Tính chất tạm thời trên RAM của iptables:**
   * Các rule `iptables` trên modem iGate GW040H được lưu trên RAM (`/proc/sys/net/ipv4/...`). Nếu modem bị mất điện hoặc khởi động lại, các rule chặn sẽ tự động biến mất.
   * *Giải pháp:* App lưu danh sách các thiết bị đang bị chặn trong `RouterStorage`. Khi phát hiện modem vừa khởi động lại (uptime ngắn lại hoặc iptables trống), App có thể tự động đồng bộ lại các rule chặn hoặc hiển thị nút "Đồng bộ lại danh sách chặn".
2. **Bảo mật mật khẩu:**
   * Mật khẩu modem được lưu trong `EncryptedSharedPreferences` thông qua Android Jetpack Security (`MasterKey`), tuyệt đối không lưu plain text.
3. **Độ ổn định của kết nối Telnet:**
   * Không giữ kết nối Telnet mở liên tục vì daemon BusyBox telnetd trên modem iGate rất yếu (chỉ chịu được 1-2 phiên đồng thời). App kết nối theo mô hình **Transient Session**: Kết nối -> Gửi lệnh -> Nhận kết quả -> `exit` và ngắt Socket. Mọi lệnh đều đi qua `Mutex` tuần tự.
