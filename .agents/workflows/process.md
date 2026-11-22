---
description: Thực thi mã nguồn Kotlin & Compose sau khi kế hoạch được duyệt, ghi log vào docs/processes/ và thông báo Telegram
---

# Mục đích
Workflow này được kích hoạt SAU KHI kế hoạch (`/plan`) đã được người dùng phê duyệt. AI Agent sẽ triển khai code và ghi lại nhật ký chi tiết vào `docs/processes/`.

# Quy tắc an toàn:
- Không tự tiện xóa bỏ các cấu trúc nền tảng đã ổn định nếu không có trong plan.
- Tuyệt đối tuân thủ triết lý Liquid Glass UI và MVVM Clean Architecture.
- Bắt buộc gửi thông báo Telegram tại 2 thời điểm: bắt đầu code và hoàn thành code.

# Các bước thực thi:
1. **Gửi thông báo Telegram BẮT ĐẦU THỰC THI:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "PROCESS_START" -TaskName "<tên_chức_năng>" -Description "Bắt đầu triển khai mã nguồn theo kế hoạch đã phê duyệt"
   ```
2. **Tiến hành chỉnh sửa/tạo mới code** theo đúng phạm vi của plan:
   - Viết ViewModel, Coroutine Flows an toàn, chạy IO thread cho các thao tác mạng/ping.
   - Viết Composable Screen & Components theo bảng màu Token, Luminous rim, bo góc mượt mà.
3. **Ghi nhật ký thực thi vào `docs/processes/`:**
   - `docs/processes/PROCESS_YYYYMMDD_{XX}_ARCHITECTURE.md`
   - `docs/processes/PROCESS_YYYYMMDD_{XX}_UI_COMPOSE.md`
   - `docs/processes/PROCESS_YYYYMMDD_{XX}_BUILD_CONFIG.md`
4. **Gửi thông báo Telegram HOÀN TẤT THỰC THI:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "PROCESS_FINISH" -TaskName "<tên_chức_năng>" -Description "Đã cập nhật mã nguồn thành công và ghi nhận nhật ký đầy đủ" -ImpactedFiles @("<danh sách file đã sửa>")
   ```
5. Báo cáo tổng thể cho người dùng và đề xuất kiểm tra build (`/build`).
