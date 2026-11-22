---
description: Xử lý sự cố/bug phát sinh ngoài plan, ghi log vào docs/fixes/ và thông báo Telegram
---

# Mục đích
Workflow xử lý các lỗi phát sinh bất ngờ, crash log, hoặc yêu cầu hotfix khẩn cấp mà không nằm trong kế hoạch ban đầu, đảm bảo lưu vết và thông báo minh bạch.

# Các bước thực thi:
1. **Gửi thông báo Telegram BẮT ĐẦU FIX:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "FIX_START" -TaskName "<tên_lỗi>" -Description "Bắt đầu điều tra sự cố: <mô tả ngắn về lỗi>"
   ```
2. **Phân tích nguyên nhân gốc rễ (Root Cause):**
   - Đọc kỹ logcat, stacktrace, kiểm tra lifecycle coroutines, nullability hoặc quyền mạng.
3. **Áp dụng bản vá code** và kiểm tra lại cục bộ.
4. **Tạo tài liệu ghi nhận vào `docs/fixes/`:**
   - `docs/fixes/FIX_YYYYMMDD_{XX}_BUGFIX.md`: Nêu rõ lỗi gặp phải, nguyên nhân, danh sách file và giải pháp đã sửa.
5. **Gửi thông báo Telegram HOÀN TẤT FIX:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "FIX_FINISH" -TaskName "<tên_lỗi>" -Description "Đã khắc phục hoàn toàn sự cố và cập nhật nhật ký vá lỗi" -ImpactedFiles @("docs/fixes/FIX_YYYYMMDD_{XX}_BUGFIX.md", "<file đã sửa>")
   ```
6. Báo cáo kết quả vá lỗi cho người dùng.
