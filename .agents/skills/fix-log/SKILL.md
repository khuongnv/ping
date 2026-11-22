---
name: fix-log
description: "Handle unexpected bug fixes in Android code and write FIX logs. Use when user says /fix, bug fix, hotfix, debug issue, sua loi."
user-invocable: true
---

# Fix Log

## Purpose
Track out-of-plan bug fixes and create formal FIX documents in `docs/fixes/`, **with mandatory Telegram start & finish notifications**.

## Procedure

### Bước 1: GỬI THÔNG BÁO BẮT ĐẦU VÁ LỖI LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "FIX_START" `
    -TaskName "<mã_hoặc_tên_lỗi>" `
    -Description "Bắt đầu điều tra nguyên nhân gốc rễ và xử lý sự cố: <mô tả ngắn về lỗi>" `
    -Details @("Phân tích stack trace / logcat / hành vi lỗi", "Khoanh vùng file mã nguồn gây ra sự cố")
```

### Bước 2: Phân tích và sửa lỗi mã nguồn
1. Xác định nguyên nhân gốc rễ (Root Cause).
2. Áp dụng bản vá tối thiểu, an toàn, tuân thủ `docs/rules/`.
3. Kiểm tra lại logic để đảm bảo không gây lỗi hồi quy (regression).

### Bước 3: Tạo nhật ký vá lỗi trong `docs/fixes/`
Tạo file: `docs/fixes/FIX_YYYYMMDD_XX_BUGFIX.md`:
- Tiêu đề lỗi và phạm vi ảnh hưởng.
- Nguyên nhân gốc rễ (Root cause).
- Danh sách tệp tin đã sửa và tóm tắt đoạn mã vá lỗi.
- Hướng dẫn kiểm thử xác nhận lỗi đã hết.

### Bước 4: GỬI THÔNG BÁO HOÀN TẤT VÁ LỖI LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "FIX_FINISH" `
    -TaskName "<mã_hoặc_tên_lỗi>" `
    -Description "Đã khắc phục thành công sự cố và cập nhật nhật ký vá lỗi" `
    -ImpactedFiles @("docs/fixes/FIX_YYYYMMDD_XX_BUGFIX.md", "<file mã nguồn đã sửa>") `
    -Details @("Đã giải quyết dứt điểm nguyên nhân gốc rễ", "Đã xác nhận tính toàn vẹn của mã nguồn") `
    -ExtraNote "Sẵn sàng chạy kiểm tra build hoặc đồng bộ git"
```

### Bước 5: Báo cáo kết quả
Báo cáo nguyên nhân và tệp tin đã chỉnh sửa cho người dùng.
