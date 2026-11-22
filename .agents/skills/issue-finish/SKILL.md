---
name: issue-finish
description: "Finish an issue by merging into develop branch, pushing to origin, cleaning local branch, and sending finish notification to Telegram. Use when user says FINISH [issue_name], hoan thanh task, ket thuc task."
user-invocable: true
---

# Issue Finish

## Purpose
Standardize closing a feature/bugfix branch, integrating into `develop`, cleaning local branch, and **automatically sending a Vietnamese completion notification to the project Telegram channel**.

## Procedure

### Bước 1: Kiểm tra trạng thái code
1. Đảm bảo toàn bộ thay đổi trên nhánh hiện tại đã được commit hoặc stash:
   ```bash
   git status
   ```
2. (Khuyến nghị) Chạy kiểm tra biên dịch trước khi merge:
   ```bash
   ./gradlew assembleDebug
   ```

### Bước 2: Tích hợp vào nhánh `develop`
1. Chuyển về nhánh `develop` và cập nhật bản mới nhất:
   ```bash
   git checkout develop
   git pull origin develop
   ```
2. Merge nhánh chức năng vào `develop`:
   ```bash
   git merge --no-ff feature/<issue_name> -m "merge: hoàn thành feature/<issue_name> vào develop"
   ```
3. Đẩy lên remote repository:
   ```bash
   git push origin develop
   ```
4. Xóa nhánh local sau khi tích hợp thành công:
   ```bash
   git branch -d feature/<issue_name>
   ```

### Bước 3: GỬI THÔNG BÁO TELEGRAM (BẮT BUỘC)
Chạy script gửi thông báo kết thúc task với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "TASK_FINISH" `
    -TaskName "<issue_name>" `
    -Branch "develop" `
    -Description "Đã hoàn thành và tích hợp thành công nhánh feature/<issue_name> vào nhánh develop" `
    -Details @("Đã kiểm tra biên dịch không có lỗi", "Đã merge mã nguồn vào develop và push lên origin", "Đã dọn dẹp nhánh cục bộ") `
    -ExtraNote "Sẵn sàng cho việc kiểm thử tích hợp hoặc đóng gói phát hành"
```

### Bước 4: Báo cáo kết quả
Báo cáo cho người dùng trạng thái merge, push và xác nhận thông báo Telegram đã gửi.
