---
description: Khởi tạo nhánh làm việc mới từ develop theo Git Branch Strategy và gửi thông báo bắt đầu task lên Telegram
---

# Mục đích
Workflow chuẩn hóa khi người dùng yêu cầu bắt đầu một task/issue mới (ví dụ: `START HNI-101-ping-chart`).

# Các bước thực thi:
1. **Lấy mã nguồn mới nhất:**
   ```bash
   git fetch origin
   ```
2. **Kiểm tra và chuẩn bị nhánh cơ sở (`develop` hoặc `main`):**
   - Nếu đã có nhánh `develop`:
     ```bash
     git checkout develop
     git pull origin develop
     ```
   - Nếu chưa có nhánh `develop`:
     ```bash
     git checkout main
     git pull origin main
     git checkout -b develop
     ```
3. **Khởi tạo nhánh tính năng:**
   ```bash
   git checkout -b feature/<issue_name>
   ```
4. **Gửi thông báo Telegram BẮT ĐẦU TASK:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "TASK_START" -TaskName "<issue_name>" -Branch "feature/<issue_name>" -Description "Bắt đầu triển khai công việc mới trên nhánh feature/<issue_name>" -Details @("Đồng bộ nhánh cơ sở develop mới nhất", "Tạo và chuyển sang nhánh làm việc độc lập feature/<issue_name>") -ExtraNote "AI Agent sẵn sàng tiếp nhận yêu cầu phân tích và lập kế hoạch /plan"
   ```
5. Báo cáo trạng thái nhánh mới cho người dùng.
