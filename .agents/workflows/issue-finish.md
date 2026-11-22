---
description: Hoàn thành task, tích hợp nhánh chức năng vào develop, push remote, dọn nhánh và gửi thông báo kết thúc lên Telegram
---

# Mục đích
Workflow chuẩn hóa khi người dùng yêu cầu kết thúc một task/issue (ví dụ: `FINISH HNI-101-ping-chart`).

# Các bước thực thi:
1. **Kiểm tra trạng thái commit trên nhánh chức năng:**
   ```bash
   git status
   ```
   (Nếu còn file chưa commit, nhắc nhở hoặc thực hiện commit trước).
2. **Chuyển về nhánh `develop` và cập nhật:**
   ```bash
   git checkout develop
   git pull origin develop
   ```
3. **Merge nhánh chức năng vào `develop`:**
   ```bash
   git merge --no-ff feature/<issue_name> -m "merge: hoàn thành feature/<issue_name> vào develop"
   ```
4. **Đẩy mã nguồn lên remote:**
   ```bash
   git push origin develop
   ```
5. **Dọn dẹp nhánh cục bộ:**
   ```bash
   git branch -d feature/<issue_name>
   ```
6. **Gửi thông báo Telegram KẾT THÚC TASK:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "TASK_FINISH" -TaskName "<issue_name>" -Branch "develop" -Description "Đã hoàn thành và tích hợp thành công nhánh feature/<issue_name> vào develop" -Details @("Toàn bộ tính năng đã được kiểm thử và tích hợp", "Đã push lên remote develop và xóa nhánh local feature/<issue_name>") -ExtraNote "Task đã chính thức hoàn tất!"
   ```
7. Báo cáo kết quả đóng task cho người dùng.
