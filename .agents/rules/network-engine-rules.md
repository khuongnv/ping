# 🌐 Quy Tắc Xây Dựng Động Cơ Đo Kiểm Mạng (Network Engine Rules)

Tài liệu này là quy chuẩn kỹ thuật bắt buộc dành cho **`network-engine-specialist`** và **`device-network-specialist`** khi thao tác với tầng mạng, luồng đo kiểm ICMP và truy vấn phần cứng.

---

## ⚡ 1. Quy Chuẩn Động Cơ Ping ICMP (`PingEngine.kt`)

### 1.1. Thực Thi Lệnh Ping Qua Shell / Process
- Khi khởi tạo tiến trình con (`Runtime.getRuntime().exec` hoặc `ProcessBuilder`):
  - Lệnh tiêu chuẩn: `ping -c <count> -s <packetsize> -W <timeout_sec> <host>` (cho Linux/Android).
  - Đọc `InputStream` và `ErrorStream` qua `BufferedReader` trong một Coroutine chạy trên `Dispatchers.IO`.
- **Cơ chế hủy tiến trình (Process Termination)**:
  - Khi người dùng bấm **Dừng** hoặc chuyển màn hình:
    - Bắt buộc gọi `process?.destroy()` (hoặc `destroyForcibly()` trên Android 8+).
    - Đóng toàn bộ `Reader` và `Stream` trong khối `finally`.
    - Hủy Coroutine Job ngay lập tức (`job?.cancel()`).
  - Tuyệt đối không để tiến trình Ping chạy mồ côi (zombie process) dưới nền gây cạn kiệt pin và CPU.

### 1.2. Phân Tích Dòng Log ICMP (Regex & Parsing)
- Xử lý tương thích định dạng output ping đa dạng giữa các phiên bản kernel Linux / Android:
  - Format 1: `64 bytes from 8.8.8.8: icmp_seq=1 ttl=117 time=14.2 ms`
  - Format 2: `from 8.8.8.8: icmp_seq=1 time=14.2ms`
  - Trường hợp lỗi: `Request timeout for icmp_seq X`, `Destination Host Unreachable`, `unknown host`.
- Khi parse thất bại: Không được ném ngoại lệ làm crash ứng dụng; ghi nhận là gói tin mất (packet drop) hoặc ghi log cảnh báo và tiếp tục phiên đo.

### 1.3. Tính Toán Chỉ Số RTT & Gói Tin
- **RTT Minimum**: Giá trị RTT nhỏ nhất ghi nhận được trong phiên hiện tại.
- **RTT Maximum**: Giá trị RTT lớn nhất ghi nhận được.
- **RTT Average**: Tính trung bình cộng của toàn bộ các gói tin phản hồi thành công (`totalRtt / receivedCount`).
- **Tỉ lệ mất gói (% Packet Loss)**: `((sentCount - receivedCount) * 100f) / sentCount`.

---

## 🛰️ 2. Quy Chuẩn Thông Tin Mạng Thiết Bị (`NetworkInfoService.kt`)

### 2.1. Quản Lý Quyền & Android API Level
- Android 10+ (API 29+): `WifiManager.getConnectionInfo()` yêu cầu quyền vị trí `ACCESS_FINE_LOCATION` để đọc tên Wi-Fi (SSID). Nếu chưa có quyền, hiển thị `<Wi-Fi Connected>` thay vì ném SecurityException.
- Sử dụng `ConnectivityManager` và `LinkProperties` để lấy chính xác:
  - Địa chỉ Gateway cục bộ (Default Route).
  - Danh sách DNS Servers (DNS1, DNS2).
  - Mặt nạ mạng (Subnet Mask qua Prefix Length).

### 2.2. Truy Vấn Public IP & Nhà Cung Cấp Mạng (ISP)
- Sử dụng OkHttp Client được cấu hình kết nối an toàn:
  - Connect Timeout: 5 giây.
  - Read Timeout: 5 giây.
  - Tự động hủy yêu cầu nếu mạng ngắt kết nối.
- Sử dụng fallback endpoints:
  - Endpoint chính: `http://ip-api.com/json/?fields=status,message,country,isp,query`
  - Endpoint dự phòng: `https://api.ipify.org?format=json`
- Bộ đệm cục bộ (In-Memory Cache): Không gọi API Public IP liên tục gây tốn bandwidth hoặc chạm rate-limit (tối đa 1 lần mỗi khi kết nối mạng thay đổi).

---

## 🛡️ 3. An Toàn Bộ Nhớ & Luồng (Thread & Memory Safety)

1. **Dispatcher Isolation**:
   - Mọi thao tác Socket, Process, OkHttp, HTTP Request BẮT BUỘC nằm trong `Dispatchers.IO`.
   - Cập nhật UI State BẮT BUỘC thực hiện trên Main Thread (qua StateFlow hoặc `withContext(Dispatchers.Main)`).
2. **Buffer Ring Memory**:
   - Dữ liệu điểm biểu đồ sóng `RealtimePingChart` chỉ giữ tối đa 60 điểm gần nhất trong danh sách. Khi thêm điểm mới, tự động xóa điểm cũ nhất (`removeFirst()`).
   - Nhật ký `TerminalLogPanel` chỉ giữ tối đa 200 dòng gần nhất để tránh OOM khi người dùng để ping liên tục hàng giờ.
