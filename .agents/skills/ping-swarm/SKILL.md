---
name: ping-swarm
description: Operational runbook and multi-agent coordination protocol for the VNPT Ping Tool Android project.
---

# VNPT Ping Tool - Swarm Coordination Skill

Skill này cung cấp cẩm nang vận hành, phân chia vai trò và các kịch bản phối hợp chuyên sâu giữa các agent trong Swarm Agent dành cho dự án **VNPT Ping Tool (Zero Ads - Liquid Glass Edition)**.

---

## 👥 Danh Sách Swarm Subagents & Vai Trò

| Subagent Name | Lĩnh vực chuyên môn | Khi nào triệu hồi? |
| :--- | :--- | :--- |
| `network-engine-specialist` | ICMP Ping Engine, Traceroute, Packet parser, Timeout & RTT stats | Khi tinh chỉnh logic ping, tính toán jitter/rtt, thêm cơ chế traceroute, sửa regex log |
| `compose-ui-specialist` | Jetpack Compose, Liquid Glass, Realtime Canvas Wave, Spring Physics | Khi sửa đổi giao diện, thiết kế card kính, tinh chỉnh biểu đồ sóng, dock bottom bar |
| `device-network-specialist` | Wi-Fi Info, Gateway, Subnet, DNS, Public IP & ISP, GitHub Auto-update | Khi xử lý thông tin mạng nội bộ/công cộng, Wi-Fi hardware state, kiểm tra update OTA |
| `qa-perf-specialist` | Hiệu năng, chống rò rỉ RAM/tiến trình, kiểm tra pin, build & release | Khi chuẩn bị release, kiểm tra compile Gradle, tối ưu hóa FPS canvas, review crash |
| `lead-architect` | Điều phối tổng thể, Clean Architecture, MVVM, quản lý logs | Khi thiết kế tính năng lớn, tái cấu trúc thư mục, khởi tạo log, điều phối swarm |

---

## 🛠 Kịch Bản Phối Hợp Mẫu (Standard Workflows)

### Kịch bản 1: Nâng cấp Biểu Đồ Sóng Realtime (Hiệu ứng Neon Pulse & Ngưỡng RTT)
1. **`lead-architect`**: Phân tích yêu cầu, xác định tác động đến `PingScreen` và `RealtimePingChart`.
2. **`compose-ui-specialist`**:
   - Thêm hiệu ứng phát xung Neon Pulse tại điểm cuối cùng trên đường vẽ Canvas.
   - Áp dụng các dải màu động theo ngưỡng độ trễ (Xanh `< 50ms`, Vàng `50-150ms`, Đỏ `> 150ms`).
3. **`network-engine-specialist`**:
   - Tối ưu hóa chu kỳ phát tín hiệu RTT qua `Flow` với nhịp độ ổn định.
4. **`qa-perf-specialist`**:
   - Kiểm tra bộ nhớ bằng cách để biểu đồ chạy liên tục trong 10 phút, đảm bảo ring-buffer giới hạn ổn định ở 60 điểm và FPS duy trì 60/120 mà không tụt khung hình.

### Kịch bản 2: Bổ Sung Tính Năng Traceroute Trực Quan Hóa Từng Hop
1. **`lead-architect`**:
   - Thiết kế model `TracerouteHop(hopNumber, ip, host, rttMs, status)` trong `data/model/Models.kt`.
2. **`network-engine-specialist`**:
   - Xây dựng phương thức `executeTraceroute(targetHost)` trong `PingEngine.kt`, gửi gói tin với TTL tăng dần từ 1 đến 30.
   - Phân tích địa chỉ phản hồi ICMP Time Exceeded để lấy danh sách router trung chuyển.
3. **`compose-ui-specialist`**:
   - Thiết kế màn hình hoặc tab Traceroute mới theo phong cách Liquid Glass với sơ đồ các chặng mạng dọc (Vertical Stepper).
4. **`qa-perf-specialist`**:
   - Kiểm tra xử lý timeout khi có router chặn phản hồi ICMP (`* * * Request timed out`).

### Kịch bản 3: Phát Hành Bản Cập Nhật Mới (Release Pipeline)
1. **`lead-architect`**: Khởi tạo log phát hành tại `docs/logs/`.
2. **`qa-perf-specialist`**:
   - Chạy lệnh kiểm tra biên dịch:
     ```powershell
     .\gradlew.bat compileDebugKotlin
     ```
   - Kích hoạt skill [deploy-app](.agents/skills/deploy-app/SKILL.md) bằng lệnh:
     ```powershell
     powershell -ExecutionPolicy Bypass -File .agents/skills/deploy-app/scripts/deploy.ps1 -BumpType patch -ReleaseTitle "Vá lỗi và cải thiện hiệu năng biểu đồ sóng"
     ```
3. **`device-network-specialist`**:
   - Xác nhận GitHub Actions build thành công APK và cơ chế auto-update trong app nhận diện được phiên bản mới từ API.

---

## 📋 Bảng Kiểm Tra Trước Khi Hoàn Thành Task (Definition of Done)

- [ ] **BẮT BUỘC**: Đã khởi tạo và cập nhật đầy đủ nhật ký thực thi dạng Markdown trong `docs/logs/YYYY-MM-DD_<task_slug>.md` và đưa vào bảng `docs/logs/README.md`.
- [ ] Tuân thủ triết lý **Zero Ads** (100% không tích hợp bất kỳ mã quảng cáo nào).
- [ ] Mọi tác vụ I/O mạng và tiến trình ICMP đều chạy trên `Dispatchers.IO`.
- [ ] Đảm bảo cơ chế đóng luồng và hủy tiến trình Ping an toàn khi người dùng thoát hoặc chuyển trạng thái.
- [ ] Thành phần giao diện đáp ứng tiêu chuẩn Liquid Glass & Apple Music HIG.
- [ ] Ứng dụng biên dịch thành công không có lỗi cú pháp hoặc crash tiềm ẩn.
