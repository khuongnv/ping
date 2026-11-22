---
description: Biên dịch ứng dụng Android APK, chạy unit test và thông báo trạng thái build lên Telegram
---

# Mục đích
Workflow kiểm tra tính toàn vẹn của mã nguồn Android, đảm bảo không có lỗi cú pháp, compile error hoặc lỗi tài nguyên trước khi hợp nhất hoặc phát hành.

# Các bước thực thi:
1. **Gửi thông báo Telegram BẮT ĐẦU BIÊN DỊCH:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "BUILD_START" -TaskName "ANDROID-BUILD" -Description "Bắt đầu kiểm tra biên dịch ứng dụng Android với Gradle"
   ```
2. **Chạy lệnh biên dịch Android:**
   ```bash
   ./gradlew assembleDebug
   ```
3. **Gửi thông báo Telegram KẾT QUẢ BIÊN DỊCH:**
   - **Nếu thành công:**
     ```powershell
     powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "BUILD_FINISH" -TaskName "ANDROID-BUILD" -Description "Biên dịch ứng dụng Android thành công 100%" -Status "🔵 Biên dịch thành công" -Details @("File APK debug đã được tạo sẵn sàng", "Không phát sinh lỗi cú pháp hay tài nguyên")
     ```
   - **Nếu có lỗi:**
     ```powershell
     powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "BUILD_FINISH" -TaskName "ANDROID-BUILD" -Title "BIÊN DỊCH THẤT BẠI" -Description "Quá trình biên dịch phát hiện lỗi trong mã nguồn hoặc tài nguyên" -Status "🔴 Biên dịch thất bại"
     ```
4. Báo cáo chi tiết cho người dùng.
