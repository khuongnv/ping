# 🏛️ QUY TẮC KIẾN TRÚC ANDROID (KOTLIN & JETPACK COMPOSE)
## Dự án: VNPT Ping Tool (`com.vnpt.pingtool`)

---

## 1. TỔNG QUAN KIẾN TRÚC

Dự án áp dụng kiến trúc **MVVM (Model - View - ViewModel)** kết hợp tư tưởng **Clean Architecture** và luồng dữ liệu đơn hướng **Unidirectional Data Flow (UDF)** trên nền tảng **Android Jetpack Compose**:

```text
┌─────────────────────────────────────────────────────────────┐
│                    UI LAYER (Jetpack Compose)               │
│  - Screens: PingScreen, TracerouteScreen, MyIpScreen...     │
│  - Components: LiquidGlassCard, LiveChart, TerminalLog...   │
│  - Theme: LiquidGlassTheme, ColorTokens, Typography         │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow (UI State)
                               │ Events (User Intents)
┌──────────────────────────────▼──────────────────────────────┐
│                       VIEWMODEL LAYER                       │
│  - PingViewModel, MyIpViewModel, TracerouteViewModel...     │
│  - Quản lý CoroutineScope (viewModelScope)                  │
│  - StateFlow<UiState> bất biến (@Immutable)                 │
└──────────────────────────────▲──────────────────────────────┘
                               │ Flow / Suspend Functions
                               │ DTO / Domain Models
┌──────────────────────────────▼──────────────────────────────┐
│                    DATA & ENGINE LAYER                      │
│  - Engine: PingEngine (ICMP process), TracerouteEngine      │
│  - Network: OkHttp, PublicIpClient, GitHubUpdateClient      │
│  - Update: AppUpdateManager (DownloadManager, FileProvider) │
│  - Model: PingResult, PingStats, TracerouteHop, IpInfo      │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. PHÂN BỔ THƯ MỤC CHUẨN (`app/src/main/java/com/vnpt/pingtool/`)

| Phân hệ / Thư mục | Trách nhiệm & Nội dung |
|:---|:---|
| `data/engine/` | Chứa engine đo kiểm mạng (ICMP Ping thực thi qua `ProcessBuilder`/`Runtime.getRuntime().exec("ping ...")`, Traceroute engine). Phải chạy hoàn toàn trên `Dispatchers.IO`. |
| `data/network/` | Chứa client HTTP (OkHttp), tra cứu Public IP, kết nối GitHub API kiểm tra bản cập nhật mới. |
| `data/model/` | Chứa các thực thể dữ liệu (`data class`) bất biến, model kết quả đo kiểm, thông tin IP, cấu hình Ping. |
| `data/update/` | Quản lý tải bản cập nhật APK nền từ GitHub Releases và khởi chạy intent cài đặt qua `FileProvider`. |
| `ui/screens/` | Các màn hình Composable chính: `PingScreen`, `TracerouteScreen`, `MyIpScreen`, `SettingsScreen`, `HistoryScreen`. |
| `ui/components/` | Các Composable dùng chung: Thẻ kính lỏng `LiquidGlassCard`, Dock nổi `FloatingBottomBar`, Biểu đồ sóng `PingWaveformCanvas`, Live terminal `TerminalLogView`. |
| `ui/theme/` | Bảng màu token, kiểu chữ Apple HIG, Liquid Glass shapes và Material3 Theme wrapper. |
| `ui/viewmodel/` | Quản lý logic nghiệp vụ và trạng thái giao diện của từng màn hình. |

---

## 3. NGUYÊN TẮC BẮT BUỘC KHI VIẾT CODE

### 3.1. An toàn luồng (Threading & Coroutines):
1. **Tuyệt đối không chạy tác vụ nặng trên Main Thread:**
   - Các lệnh Ping ICMP, Traceroute, DNS lookup, HTTP request bắt buộc phải bao bọc trong `withContext(Dispatchers.IO)`.
   - Kết quả phát ra UI dưới dạng `Flow<T>` hoặc `emit()` qua StateFlow.
2. **Hủy tác vụ an toàn khi thoát màn hình:**
   - Khi người dùng bấm Dừng Ping hoặc chuyển màn hình, `Job` đo kiểm phải được `cancel()` ngay lập tức để giải phóng tiến trình hệ điều hành (Process).

### 3.2. Quản lý trạng thái Jetpack Compose:
1. **UiState bất biến (@Immutable):**
   - Mọi trạng thái màn hình phải được định nghĩa dưới dạng `data class` với annotation `@Immutable` để Compose tối ưu hóa Skip Recomposition.
2. **Tách biệt State và Event:**
   - ViewModel chỉ expose `StateFlow<UiState>`.
   - UI chỉ gọi hàm sự kiện của ViewModel (ví dụ: `viewModel.startPing(host)`, `viewModel.stopPing()`).
3. **Tránh Memory Leak:**
   - Không truyền `Context` vào ViewModel. Sử dụng `AndroidViewModel(application)` nếu bắt buộc cần Application Context (như truy cập `ConnectivityManager`, `WifiManager`).

### 3.3. Xử lý ngoại lệ và an toàn hệ thống (Fault Tolerance):
1. **ICMP Ping Android compatibility:**
   - Một số thiết bị hạn chế quyền socket ICMP raw, do đó engine phải sử dụng lệnh `ping -c ...` của hệ điều hành với timeout chuẩn và xử lý bắt exception `IOException`, `InterruptedException`.
2. **Kiểm tra kết nối mạng:**
   - Luôn kiểm tra trạng thái mạng trước khi thực hiện đo kiểm, cung cấp thông báo lỗi tiếng Việt thân thiện khi không có mạng hoặc máy chủ không phản hồi.

---

## 4. QUY CHUẨN ĐẶT TÊN (NAMING CONVENTIONS)

- **Class / Composable:** PascalCase (ví dụ: `PingScreen`, `PingEngine`, `PingViewModel`).
- **Function / Method:** camelCase (ví dụ: `startPingSession()`, `fetchPublicIp()`).
- **UiState:** `<Feature>UiState` (ví dụ: `PingUiState`, `MyIpUiState`).
- **Composable Components:** Luôn có tiền tố hoặc hậu tố mô tả bản chất UI (ví dụ: `LiquidGlassCard`, `FloatingBottomBar`, `WaveformChart`).
- **Resource id / xml:** snake_case (ví dụ: `ic_ping_active.xml`, `network_security_config.xml`).