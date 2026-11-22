---
name: plan-docs
description: "Create implementation plans for Android modules (Architecture/Engine, UI Compose, Build Config). Use when user says /plan, lap ke hoach, viet tai lieu trien khai."
user-invocable: true
---

# Plan Docs

## Purpose
Create formal implementation plans before modifying any Android source code, and **send Vietnamese start & finish notifications to Telegram**.

## Mandatory Safety Rules
- ⚠️ **KHI LẬP PLAN: TUYỆT ĐỐI KHÔNG SỬA MÃ NGUỒN HOẶC CHẠY LỆNH GÂY BIẾN ĐỔI ỨNG DỤNG.**
- Đọc và tuân thủ các quy chuẩn trong:
  - `docs/rules/rule_android_architecture.md`
  - `docs/rules/rule_ui_ux_liquid_glass.md`
  - `docs/rules/rule_git_deployment.md`
  - `docs/rules/rule_telegram_notifications.md`

## Procedure

### Bước 1: GỬI THÔNG BÁO BẮT ĐẦU PLAN LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "PLAN_START" `
    -TaskName "<tên_chức_năng>" `
    -Description "Bắt đầu phân tích yêu cầu và lập kế hoạch triển khai cho chức năng: <tên_chức_năng>" `
    -Details @("Đang rà soát cấu trúc thư mục và quy tắc kiến trúc", "Chuẩn bị tài liệu kỹ thuật trong docs/plans/")
```

### Bước 2: Phân tích và tạo tài liệu Plan
Phân tách kế hoạch thành tối đa 3 file riêng biệt trong `docs/plans/` cho các phân hệ bị ảnh hưởng (chỉ tạo file bị ảnh hưởng, không tạo file rỗng):
1. `docs/plans/PLAN_YYYYMMDD_XX_ARCHITECTURE.md`:
   - Phân hệ Data, ICMP Ping Engine, Traceroute Engine, OkHttp Network client, Models, ViewModel State management.
2. `docs/plans/PLAN_YYYYMMDD_XX_UI_COMPOSE.md`:
   - Màn hình Compose, Liquid Glass Cards, Apple Music HIG design, Spring animations, Canvas Waveform chart.
3. `docs/plans/PLAN_YYYYMMDD_XX_BUILD_CONFIG.md`:
   - Gradle dependencies, AndroidManifest permissions, ProGuard, GitHub Actions CI/CD.

*(Trong đó `YYYYMMDD` là ngày hiện tại, `XX` là số thứ tự `01`, `02`...)*

### Bước 3: GỬI THÔNG BÁO HOÀN TẤT PLAN LÊN TELEGRAM
Sau khi ghi xong các file plan, chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "PLAN_FINISH" `
    -TaskName "<tên_chức_năng>" `
    -Description "Đã hoàn tất tài liệu kế hoạch triển khai cho chức năng: <tên_chức_năng>" `
    -ImpactedFiles @("docs/plans/PLAN_YYYYMMDD_XX_...") `
    -Status "Chờ người dùng phê duyệt" `
    -ExtraNote "Vui lòng xem xét các tài liệu plan trong docs/plans/ và gõ lệnh /process để phê duyệt triển khai"
```

### Bước 4: Nhắc nhở người dùng
Thông báo cho người dùng danh sách file plan đã tạo và chờ người dùng xác nhận trước khi code.
