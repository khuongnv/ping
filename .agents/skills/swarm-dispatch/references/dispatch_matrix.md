# 📋 Ma Trận Phân Bổ & Điều Phối Swarm Agent (Dispatch Matrix)

Bảng đối chiếu nhanh để `lead-architect` phân luồng công việc chính xác đến từng Subagent.

---

## 🗂️ Ma Trận Điều Phối Nhiệm Vụ

| Lĩnh Vực / Loại Nhiệm Vụ | Agent Chủ Trì | Agent Phối Hợp | Tệp Tin & Thư Mục Trọng Tâm |
| :--- | :--- | :--- | :--- |
| **ICMP Ping & Động Cơ Đo** | `network-engine-specialist` | `qa-perf-specialist` | `data/engine/PingEngine.kt`<br>`data/model/Models.kt` |
| **Traceroute & Hop-by-Hop** | `network-engine-specialist` | `compose-ui-specialist` | `data/engine/PingEngine.kt`<br>`ui/screens/` |
| **Biểu Đồ Sóng Canvas Realtime** | `compose-ui-specialist` | `qa-perf-specialist` | `ui/components/RealtimePingChart.kt` |
| **Thẻ Kính Lỏng & Bottom Bar** | `compose-ui-specialist` | `lead-architect` | `ui/components/LiquidGlassCard.kt`<br>`ui/components/FloatingGlassBottomBar.kt` |
| **Màn Hình Ping / History / Settings** | `compose-ui-specialist` | `network-engine-specialist` | `ui/screens/*` |
| **Wi-Fi, Gateway, DNS, Subnet Mask** | `device-network-specialist` | `compose-ui-specialist` | `data/network/NetworkInfoService.kt`<br>`ui/screens/MyIpScreen.kt` |
| **Public IP & Tra Cứu ISP** | `device-network-specialist` | `qa-perf-specialist` | `data/network/NetworkInfoService.kt` |
| **Kiểm Tra & Tải Bản Cập Nhật OTA** | `device-network-specialist` | `qa-perf-specialist` | `data/update/GitHubUpdateManager.kt`<br>`ui/screens/SettingsScreen.kt` |
| **Tối Ưu Hiệu Năng & Bộ Nhớ RAM** | `qa-perf-specialist` | `compose-ui-specialist` | `ui/components/RealtimePingChart.kt`<br>`data/engine/PingEngine.kt` |
| **Kiểm Tra Build & Phát Hành (Deploy)** | `qa-perf-specialist` | `lead-architect` | `.agents/skills/deploy-app/`<br>`app/build.gradle.kts`<br>`.github/workflows/release.yml` |
| **Tái Cấu Trúc Toàn Dự Án / DI** | `lead-architect` | Toàn bộ Swarm | `MainActivity.kt`<br>`app/build.gradle.kts`<br>`ui/viewmodel/` |
