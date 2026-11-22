---
description: Lập kế hoạch triển khai tính năng Android theo các module Architecture/Engine, UI Compose, Build Config và gửi thông báo Telegram
---

# Mục đích
Workflow này thiết lập quy trình bắt buộc khi được yêu cầu "lên plan" (lập kế hoạch) hoặc "viết tài liệu triển khai" cho ứng dụng VNPT Ping Tool.

# Quy tắc an toàn bắt buộc:
- ⚠️ **KHI THỰC HIỆN WORKFLOW NÀY: CHỈ ĐƯỢC PHÉP LÊN PLAN, TUYỆT ĐỐI KHÔNG SỬA CODE HOẶC CHẠY LỆNH GÂY BIẾN ĐỔI MÃ NGUỒN.**
- Phải đọc và tuân thủ các quy chuẩn trong `docs/rules/`:
  - `docs/rules/rule_android_architecture.md`
  - `docs/rules/rule_ui_ux_liquid_glass.md`
  - `docs/rules/rule_git_deployment.md`
  - `docs/rules/rule_telegram_notifications.md`

# Các bước thực thi:
1. **Gửi thông báo Telegram BẮT ĐẦU PLAN:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "PLAN_START" -TaskName "<tên_chức_năng>" -Description "Bắt đầu phân tích & lập kế hoạch triển khai: <tên_chức_năng>"
   ```
2. **Khảo sát mã nguồn hiện tại** và đối chiếu với yêu cầu tính năng mới.
3. **Phân tách kế hoạch thành tối đa 3 file** trong `docs/plans/` (chỉ tạo file có ảnh hưởng):
   - `docs/plans/PLAN_YYYYMMDD_{XX}_ARCHITECTURE.md`: Engine ICMP/Traceroute, Network OkHttp, Data Models, ViewModel, Coroutines/StateFlow.
   - `docs/plans/PLAN_YYYYMMDD_{XX}_UI_COMPOSE.md`: Giao diện Compose, Liquid Glass Card, Canvas Realtime Chart, Spring animations.
   - `docs/plans/PLAN_YYYYMMDD_{XX}_BUILD_CONFIG.md`: Quyền Android, Gradle dependencies, ProGuard, GitHub Actions.
4. **Gửi thông báo Telegram HOÀN TẤT PLAN:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "PLAN_FINISH" -TaskName "<tên_chức_năng>" -Description "Đã lập xong tài liệu plan cho <tên_chức_năng>" -ImpactedFiles @("docs/plans/PLAN_YYYYMMDD_{XX}_...")
   ```
5. Nhắc người dùng kiểm tra file plan và xác nhận trước khi chuyển sang bước thực thi (`/process`).
