---
name: issue-start
description: "Start a new issue/feature branch and send start notification to Telegram. Use when user says START [issue_name], bat dau task, tao nhanh moi."
user-invocable: true
---

# Issue Start

## Purpose
Standardize starting a new work item/issue branch following `docs/GIT_BRANCH_STRATEGY.md`, and **automatically send a Vietnamese start notification to the project Telegram channel**.

## Procedure

### Bước 1: Chuẩn bị nhánh nền tảng
1. Kiểm tra nhánh hiện tại và lấy mã nguồn mới nhất:
   ```bash
   git fetch origin
   ```
2. Nếu nhánh `develop` đã tồn tại:
   ```bash
   git checkout develop
   git pull origin develop
   ```
   Nếu nhánh `develop` chưa có trên remote, tạo `develop` từ `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b develop
   ```

### Bước 2: Tạo nhánh mới theo quy chuẩn
Xác định tiền tố phù hợp:
- `feature/<issue_name>`: Phát triển tính năng mới.
- `bugfix/<issue_name>`: Sửa lỗi trong quá trình phát triển.
- `hotfix/<issue_name>`: Sửa lỗi khẩn cấp trên Production (tạo từ `main`).

Tạo và chuyển sang nhánh mới:
```bash
git checkout -b feature/<issue_name>
```

### Bước 3: GỬI THÔNG BÁO TELEGRAM (BẮT BUỘC)
Chạy script gửi thông báo bắt đầu task với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "TASK_START" `
    -TaskName "<issue_name>" `
    -Branch "feature/<issue_name>" `
    -Description "Bắt đầu triển khai công việc mới trên nhánh feature/<issue_name>" `
    -Details @("Đã đồng bộ nhánh cơ sở", "Khởi tạo nhánh làm việc độc lập theo Git Branch Strategy") `
    -ExtraNote "AI Agent đang tiến hành phân tích mã nguồn và lập kế hoạch chi tiết"
```

### Bước 4: Báo cáo cho người dùng
Báo cáo nhánh đã được khởi tạo thành công và thông báo Telegram đã được gửi đi.
