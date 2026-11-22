---
description: Chuẩn hóa commit note tiếng Việt, thực hiện commit & push lên git repository và thông báo Telegram
---

# Mục đích
Workflow đồng bộ toàn bộ thay đổi mã nguồn và tài liệu lên remote repository, tuân thủ Conventional Commits tiếng Việt và thông báo lên Telegram.

# Các bước thực thi:
1. Kiểm tra trạng thái thay đổi:
   ```bash
   git status
   ```
2. Phân loại commit theo `docs/rules/rule_git_deployment.md`:
   - Chuẩn bị tiêu đề ngắn gọn tiếng Việt (ví dụ: `feat(ping): thêm bộ đếm gói tin realtime`).
   - Chuẩn bị mô tả chi tiết các file và chức năng đã hoàn thành.
3. Chạy commit & push:
   ```bash
   git add .
   git commit -m "<Tiêu đề tiếng Việt>" -m "<Chi tiết tiếng Việt>"
   git push
   ```
4. **Gửi thông báo Telegram ĐỒNG BỘ CODE:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "GIT_SYNC" -TaskName "<Tiêu đề commit>" -Description "Đã đồng bộ thành công mã nguồn lên GitHub" -Details @("Đã kiểm tra và commit đầy đủ các thay đổi", "Đã push lên nhánh hiện tại an toàn")
   ```
5. Báo cáo hash commit và thông tin push cho người dùng.
