---
name: process-log
description: "Implement Android code after approved plan and write PROCESS execution logs. Use when user says /process, thuc thi plan, ghi nhat ky thuc thi."
user-invocable: true
---

# Process Log

## Purpose
Implement approved plan and produce execution logs for traceability, **with mandatory Telegram start & finish notifications**.

## Safety Rules
- Chỉ thực hiện sau khi kế hoạch trong `docs/plans/` đã được người dùng phê duyệt.
- Tuân thủ nghiêm ngặt `docs/rules/rule_android_architecture.md` và `docs/rules/rule_ui_ux_liquid_glass.md`.
- Không tự động commit/push Git nếu chưa có lệnh `/git` từ người dùng.

## Procedure

### Bước 1: GỬI THÔNG BÁO BẮT ĐẦU THỰC THI LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "PROCESS_START" `
    -TaskName "<tên_chức_năng>" `
    -Description "Bắt đầu triển khai mã nguồn Kotlin & Jetpack Compose theo kế hoạch đã phê duyệt" `
    -Details @("Đọc tài liệu docs/plans/ liên quan", "Tuân thủ kiến trúc MVVM và quy chuẩn Liquid Glass UI")
```

### Bước 2: Triển khai mã nguồn
1. Thực hiện các chỉnh sửa, tạo mới mã nguồn (Screens, Components, ViewModel, Engine, Network, Model).
2. Kiểm tra format và logic không có lỗi cú pháp.

### Bước 3: Ghi nhận nhật ký thực thi vào `docs/processes/`
Tạo các file nhật ký tương ứng với các module bị tác động:
1. `docs/processes/PROCESS_YYYYMMDD_XX_ARCHITECTURE.md`:
   - Danh sách file thay đổi, tóm tắt diff, cấu trúc luồng Coroutines/StateFlow.
2. `docs/processes/PROCESS_YYYYMMDD_XX_UI_COMPOSE.md`:
   - Thay đổi giao diện Compose, token màu sắc, hiệu ứng Spring/Canvas.
3. `docs/processes/PROCESS_YYYYMMDD_XX_BUILD_CONFIG.md`:
   - Dependencies hoặc quyền mới thêm vào nếu có.

### Bước 4: GỬI THÔNG BÁO HOÀN TẤT THỰC THI LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "PROCESS_FINISH" `
    -TaskName "<tên_chức_năng>" `
    -Description "Đã hoàn tất chỉnh sửa mã nguồn và lập nhật ký thực thi thành công" `
    -ImpactedFiles @("docs/processes/PROCESS_YYYYMMDD_XX_...", "<các file code đã sửa>") `
    -Details @("Mã nguồn đã được cập nhật chuẩn xác", "Nhật ký thực thi đã lưu vết đầy đủ trong docs/processes/") `
    -ExtraNote "Người dùng có thể kiểm tra ứng dụng hoặc yêu cầu kiểm tra build / chạy thử"
```

### Bước 5: Báo cáo cho người dùng
Liệt kê các file code đã sửa và các file log đã tạo trong `docs/processes/`.
