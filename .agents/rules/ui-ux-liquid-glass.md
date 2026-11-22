# 💎 Quy Tắc Thiết Kế UI/UX Liquid Glass x Apple Music HIG

Tài liệu này là quy chuẩn bắt buộc dành cho **`compose-ui-specialist`** và toàn bộ Swarm Agent khi xây dựng hoặc tinh chỉnh giao diện cho dự án **VNPT Ping Tool**.

---

## 🎨 1. Hệ Thống Màu Sắc & Bảng Token (Design Tokens)

- **Chủ đề chủ đạo**: Dark Mode AMOLED chuyên nghiệp.
- **Nền gốc (Background)**: `#000000` (Đen tuyền 100%, không dùng màu xám nhờ nhờ).
- **Màu nhấn thương hiệu (Primary Accent)**:
  - `#007AFF` (Apple System Blue) hoặc `#0A84FF` cho các chi tiết mạng tiêu chuẩn VNPT.
  - `#FA2D55` (Apple Red) cho điểm nhấn đặc biệt / cảnh báo / nút dừng.
- **Màu bề mặt kính (Surface Glass)**:
  - Nền thẻ (Liquid Glass Card): `#1C1C1E` với độ trong suốt `alpha = 0.70f - 0.85f`.
  - Thanh điều hướng đáy nổi (Floating Dock): `#161618` với `alpha = 0.88f`.
- **Viền phản quang (Luminous Rim)**:
  - Đường viền thẻ kính: `0.75dp` với dải màu phát sáng `BorderStroke(0.75.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))))`.
- **Đổ bóng đa tầng (Elevation & Glow)**:
  - Đổ bóng mềm đa tầng với hiệu ứng bóng mờ xung quanh khi thẻ active.

---

## 📈 2. Quy Chuẩn Màu Sắc Đo Kiểm Mạng (Ping Latency Thresholds)

Biểu đồ sóng thời gian thực (`RealtimePingChart`) và các thẻ chỉ số độ trễ RTT bắt buộc tuân theo dải màu trực quan 3 cấp độ:

| Ngưỡng Độ Trễ (RTT) | Mã Màu Chuẩn | Trạng Thái Mạng | Đánh Giá Kỹ Thuật |
| :--- | :--- | :--- | :--- |
| **RTT < 50ms** | `#34C759` (Apple Neon Green) | 🟢 Xuất sắc | Rất tốt cho Game Online, Voice Call, Video 4K |
| **50ms <= RTT <= 150ms** | `#FF9F0A` (Apple Amber Yellow) | 🟡 Trung bình | Độ trễ chấp nhận được cho lướt web, streaming |
| **RTT > 150ms** hoặc **Mất gói** | `#FF453A` (Apple Vivid Red) | 🔴 Báo động | Mạng lag nghiêm trọng, nghẽn đường truyền |

---

## 🫧 3. Cấu Trúc Thành Phần Kính Lỏng (Liquid Glass Components)

### 3.1. Thẻ Kính Lỏng (`LiquidGlassCard`)
- Bo góc lớn: `20.dp` đến `24.dp` (Apple Continuous Squircle).
- Viền nổi phản quang: `0.75dp`.
- Padding thông thoáng: `16.dp` đến `20.dp`.
- Tránh đặt thẻ chồng thẻ không cần thiết; tôn trọng nguyên lý **Content-First**.

### 3.2. Thanh Điều Hướng Đáy Nổi (`FloatingGlassBottomBar`)
- Vị trí: Treo lơ lửng cách mép đáy màn hình `16.dp` đến `24.dp`.
- Nền kính mờ bán trong suốt kết hợp bo tròn hoàn toàn dạng viên thuốc (Pill shape `corner = 32.dp`).
- Con trượt chọn tab (Indicator): Viên thuốc Capsule Pill chuyển động đàn hồi tự nhiên bằng **Spring Physics** (`spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`).

### 3.3. Biểu Đồ Sóng Thời Gian Thực (`RealtimePingChart`)
- Vẽ trực tiếp trên Canvas Jetpack Compose.
- Cập nhật mượt mà 60/120 FPS.
- Giới hạn bộ đệm tối đa 50 - 100 điểm mẫu gần nhất (Ring Buffer) để giữ bộ nhớ ổn định và không làm nóng máy.
- Gradient phủ đáy (Area Fill): Chuyển màu từ màu trạng thái (Green/Yellow/Red) với `alpha = 0.35f` xuống `alpha = 0.00f` ở đáy biểu đồ.
- Điểm chốt hiện tại (Current Pulse Dot): Có vòng tròn phát xung (pulsing halo animation) tại điểm dữ liệu mới nhất.

### 3.4. Terminal Live Log Panel (`TerminalLogPanel`)
- Phong cách giao diện dòng lệnh kỹ thuật (Console/Terminal).
- Phông chữ đơn cách (Monospace font: `FontFamily.Monospace`).
- Tự động cuộn xuống dòng mới nhất khi có bản ghi ICMP mới (`LaunchedEffect` scroll to bottom).
- Nền đen than chì sâu `#0D0D0E` viền `0.5dp` xám mờ.

---

## 🚫 4. Các Điều Cấm Kỵ Trong Giao Diện (Prohibitions)

1. ❌ **CẤM tích hợp banner hoặc popup quảng cáo**: Cam kết Zero Ads 100%.
2. ❌ **CẤM chuyển động cứng đơ (Linear Animation)**: Mọi chuyển động tab, nút bấm hoặc đồ thị phải dùng `Spring Physics`.
3. ❌ **CẤM dùng màu xám nhạt làm nền Dark Mode**: Bắt buộc nền AMOLED `#000000` để tiết kiệm pin trên màn hình OLED và tạo độ sâu kính lỏng.
4. ❌ **CẤM rò rỉ recomposition lãng phí**: Tránh tạo Lambda mới hoặc State không có `remember` bên trong Composable render thường xuyên (đặc biệt trong Canvas chart và Terminal log).
