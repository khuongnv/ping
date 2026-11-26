# Kế Hoạch Triển Khai Giao Diện Compose: VNPT Router Controller (Liquid Glass x Apple Music HIG)

- **Mã kế hoạch:** `PLAN_20261126_01_UI_COMPOSE`
- **Mục tiêu:** Thay thế hoàn toàn giao diện `ScanScreen.kt` bằng `RouterManagerScreen.kt`. Cập nhật thanh điều hướng đáy `FloatingGlassBottomBar` và hệ thống Dialog quản lý Router/AP.
- **Trạng thái:** Chờ phê duyệt (Pending Approval)
- **Quy chuẩn tuân thủ:** AmoledBlack (`#000000`), Liquid Glass Cards bán trong suốt, Luminous Rim `0.75dp`, Spring Physics, Zero Lag Recomposition.

---

## 1. Tái Cấu Trúc Điều Hướng (Navigation & Bottom Bar)

### 1.1. Cập nhật `FloatingGlassBottomBar`
* **Trước đây:** 3 Tabs gồm `Ping` (Icon `NetworkCheck`), `Telnet` (Icon `Lan`), `Scan` (Icon `WifiFind`).
* **Sau khi thay đổi:**
  * Tab 1: **Ping** (`route = "ping"`, icon `Icons.Default.NetworkCheck`)
  * Tab 2: **Telnet** (`route = "telnet"`, icon `Icons.Default.Lan`)
  * Tab 3: **Router** (`route = "router"`, icon `Icons.Default.Router` hoặc `Icons.Default.SettingsEthernet`) — *Thay thế hoàn toàn tab Scan*.

### 1.2. Cập nhật `MainActivity.kt`
* Xóa bỏ việc render `ScanScreen` khi `currentRoute == "scan"`.
* Bổ sung route `"router"` điều hướng vào `RouterManagerScreen`.

---

## 2. Thiết Kế Màn Hình Chính: `RouterManagerScreen.kt`

Giao diện được chia thành các khối phân tầng Liquid Glass mượt mà:

```text
┌────────────────────────────────────────────────────────┐
│ [☰]  QUẢN TRỊ ROUTER VNPT                [⚙ Cấu hình] │
├────────────────────────────────────────────────────────┤
│ ┌── THẺ THÔNG TIN MODEM CHÍNH & QUANG (GW040H) ──────┐ │
│ │ • Gateway: 192.168.1.1 (Đã kết nối)               │ │
│ │ • Công suất thu Rx: -18.42 dBm [🟢 Tuyệt vời]      │ │
│ │ • Công suất phát Tx: 2.15 dBm | Nhiệt độ: 45°C     │ │
│ └────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────┤
│ ┌── BỘ LỌC SUB-TABS (Capsule Pill Tabs) ─────────────┐ │
│ │ [Tất cả (12)]  [Đang chặn (2)]  [Ưu tiên (3)]     │ │
│ │ [Lịch chặn (1)] [Bộ phát phụ (1)]                  │ │
│ └────────────────────────────────────────────────────┘ │
├────────────────────────────────────────────────────────┤
│ ┌── DANH SÁCH THIẾT BỊ (LazyColumn) ─────────────────┐ │
│ │ ┌── Thẻ Thiết Bị 1 (LiquidGlassCard) ────────────┐ │ │
│ │ │ 💻 MacBook Pro (Ba)             [⭐ VIP]       │ │ │
│ │ │ 192.168.1.105 • 34:AB:CD:...                   │ │ │
│ │ │ Kết nối: Wi-Fi Chính (5GHz)    [Switch: MỞ]   │ │ │
│ │ └────────────────────────────────────────────────┘ │ │
│ │ ┌── Thẻ Thiết Bị 2 ──────────────────────────────┐ │ │
│ │ │ 📱 iPad của Bin (Con)          [⏰ Lịch 22h]   │ │ │
│ │ │ 192.168.1.142 • BC:D1:22:...                   │ │ │
│ │ │ Kết nối: Xiaomi AX1800         [Switch: CHẶN]  │ │ │
│ │ └────────────────────────────────────────────────┘ │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

## 3. Các Thành Phần Giao Diện Chi Tiết (UI Components)

### 3.1. Thẻ Sức Khỏe Quang & Modem (`OpticalHealthCard.kt`)
* Nền kính mờ bán trong suốt `SurfaceCardDark` (`#16161A` với alpha `0.85`), viền phản quang `0.75dp`.
* Đo vạch sóng công suất quang `RxPower`:
  * `< -27 dBm`: 🔴 Đỏ (Cảnh báo suy hao nặng, đứt cáp).
  * `-24` đến `-27 dBm`: 🟡 Vàng (Chấp nhận được).
  * `-15` đến `-24 dBm`: 🟢 Xanh lục neon (Đạt chuẩn kỹ thuật VNPT).
* Nút "Làm mới nhanh (Quick Refresh)" gửi lệnh đọc tức thì.

### 3.2. Thẻ Thông Tin Bộ Phát Phụ (`ApNodeInfoCard.kt`)
* Hiển thị danh sách các AP / Mesh (ví dụ Xiaomi AX1800, Deco, TP-Link).
* Hiển thị IP bộ phát phụ trong mạng LAN (`192.168.1.x`), MAC.
* Nút bấm tiện ích: **"Mở Web Quản Trị AP"** (Mở trình duyệt trực tiếp vào trang cấu hình AP phụ).
* Nút thêm bộ phát phụ mới (`+ Thêm AP phụ`).

### 3.3. Thẻ Thiết Bị Kết Nối (`RouterDeviceItemCard.kt`)
* **Hiển thị định danh:**
  * Icon thiết bị thông minh (Tự nhận diện theo Vendor/Hostname: Laptop, Mobile, TV, Printer, Camera, IoT).
  * Tên gợi nhớ tùy chỉnh (Chạm vào để sửa tên: ví dụ "Điện thoại mẹ").
  * IP & MAC (Bấm vào để copy nhanh).
* **Trạng thái & Huy hiệu (Badges):**
  * `[⭐ Ưu tiên / VIP]`: Hiển thị vương miện vàng, chống chặn nhầm.
  * `[🚫 Đang chặn]`: Viền thẻ chuyển sang viền đỏ mờ.
  * `[⏰ Có lịch hẹn]`: Hiển thị khung giờ chặn tự động.
* **Hành động tức thì (Instant Toggle Switch):**
  * Gạt sang Đỏ -> Chặn ngay lập tức (App hiện feedback rung nhẹ Haptic).
  * Gạt sang Xanh -> Mở mạng ngay lập tức.
  * Nút menu 3 chấm (`...`):
    * Đặt tên gợi nhớ (Rename).
    * Thêm vào / Xóa khỏi Danh sách ưu tiên (Toggle VIP).
    * Cài đặt lịch chặn theo giờ cho thiết bị này.

### 3.4. Dialog Cài Đặt Kết Nối Modem (`RouterConfigDialog.kt`)
* Nhập IP Gateway (mặc định tự điền theo Wi-Fi hiện tại).
* Nhập Port (mặc định `23`).
* Nhập Username & Password.
* Nút **"Kiểm Tra Kết Nối (Test Connection)"** hiển thị kết quả kiểm tra trạng thái Telnet theo thời gian thực (Đang kết nối -> Đăng nhập thành công / Thất bại).

### 3.5. Dialog Thiết Lập Lịch Chặn Theo Giờ (`ScheduleRuleDialog.kt`)
* Cho phép chọn thiết bị cần áp dụng.
* Bộ chọn giờ bắt đầu (`TimePicker`: ví dụ `22:00`).
* Bộ chọn giờ kết thúc (`TimePicker`: ví dụ `06:00`).
* Bộ chọn ngày lặp lại trong tuần (T2, T3, T4, T5, T6, T7, CN).
* Switch kích hoạt / tạm ngưng lịch hẹn.

---

## 4. Tác Động Mã Nguồn UI Dự Kiến

1. `app/src/main/java/com/vnpt/pingtool/ui/screens/RouterManagerScreen.kt` (Tạo mới thay thế `ScanScreen.kt`).
2. `app/src/main/java/com/vnpt/pingtool/ui/components/RouterDeviceItemCard.kt` (Tạo mới).
3. `app/src/main/java/com/vnpt/pingtool/ui/components/OpticalHealthCard.kt` (Tạo mới).
4. `app/src/main/java/com/vnpt/pingtool/ui/components/ApNodeInfoCard.kt` (Tạo mới).
5. `app/src/main/java/com/vnpt/pingtool/ui/components/RouterConfigDialog.kt` (Tạo mới).
6. `app/src/main/java/com/vnpt/pingtool/ui/components/ScheduleRuleDialog.kt` (Tạo mới).
7. `app/src/main/java/com/vnpt/pingtool/ui/components/FloatingGlassBottomBar.kt`: Đổi tab `scan` thành `router`.
8. `app/src/main/java/com/vnpt/pingtool/MainActivity.kt`: Đổi định tuyến tab 3 sang `RouterManagerScreen`.
9. `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt`: Xóa bỏ an toàn.
