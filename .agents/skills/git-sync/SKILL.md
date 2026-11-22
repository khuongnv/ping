---
name: git-sync
description: "Prepare professional Vietnamese commit notes, commit and push to remote repository, and notify Telegram. Use when user says /git, commit push, dong bo git, day code."
user-invocable: true
---

# Git Sync

## Purpose
Standardize commit messages following Conventional Commits in Vietnamese, push code safely to remote, and **send Vietnamese synchronization notification to Telegram**.

## Procedure

### Bước 1: Kiểm tra trạng thái thay đổi
Chạy lệnh kiểm tra các file đang sửa/thêm mới:
```bash
git status
```

### Bước 2: Chuẩn bị thông điệp Commit tiếng Việt chuẩn
1. Phân loại type theo `docs/rules/rule_git_deployment.md`:
   - `feat(...)`: Tính năng mới
   - `fix(...)`: Sửa lỗi
   - `refactor(...)`: Tái cấu trúc
   - `perf(...)`: Tối ưu hiệu năng
   - `chore(...)`: Cấu hình / thư viện
   - `docs(...)`: Tài liệu / logs
2. Tiêu đề ngắn gọn, chuẩn nghiệp vụ tiếng Việt.
3. Mô tả chi tiết các điểm thay đổi.

### Bước 3: Thực hiện Commit và Push
```bash
git add .
git commit -m "<type>(<scope>): <tiêu đề ngắn tiếng Việt>" -m "<chi tiết thay đổi tiếng Việt>"
git push
```

### Bước 4: GỬI THÔNG BÁO TELEGRAM (BẮT BUỘC)
Chạy script gửi thông báo đồng bộ code với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "GIT_SYNC" `
    -TaskName "<tên_công_việc_hoặc_commit_title>" `
    -Description "Đã đồng bộ mã nguồn thành công lên kho lưu trữ từ xa (remote git)" `
    -Details @("Commit: <tiêu đề commit>", "Nhánh hiện tại đã được đẩy lên origin thành công") `
    -ExtraNote "Mã nguồn đã an toàn trên remote repository"
```

### Bước 5: Báo cáo trạng thái cho người dùng
Báo cáo hash commit, branch và xác nhận thông báo Telegram đã được phát đi.
