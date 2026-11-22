---
name: archive-history
description: "Archive docs/plans, docs/processes, docs/fixes into one HISTORY file and clean source files. Use when user says /history, archive logs, tong hop lich su."
user-invocable: true
---

# Archive History

## Purpose
Consolidate plan, process, and fix markdown logs into a single long-term history file in `docs/history/`, and **send a summary notification to Telegram**.

## Procedure

### Bước 1: Quét và đọc tất cả các file log
1. Đọc tất cả các file `.md` trong:
   - `docs/plans/` (ngoại trừ `.gitkeep`)
   - `docs/processes/` (ngoại trừ `.gitkeep`)
   - `docs/fixes/` (ngoại trừ `.gitkeep`)
2. Nếu cả 3 thư mục đều trống, thông báo cho người dùng và dừng lại.

### Bước 2: Tổng hợp thành file lịch sử duy nhất
Tạo file `docs/history/HISTORY_YYYYMMDD.md` (nếu trùng ngày thì đặt `_v2`, `_v3`...):
- Bảng tóm tắt các tính năng / đợt vá lỗi đã thực hiện.
- Tóm tắt quyết định kiến trúc từ các Plan.
- Tóm tắt danh sách file và thay đổi từ các Process và Fix.
- Bảo toàn bối cảnh kỹ thuật, không sao chép nguyên văn dàn trải.

### Bước 3: Xin xác nhận người dùng trước khi dọn file nguồn
- **BẮT BUỘC:** Hỏi người dùng xem có muốn xóa các file con nguồn trong `docs/plans/`, `docs/processes/`, `docs/fixes/` hay không.
- Chỉ xóa khi người dùng đồng ý; tuyệt đối không xóa thư mục hay file `.gitkeep`.

### Bước 4: GỬI THÔNG BÁO TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "CUSTOM" `
    -TaskName "ARCHIVE-HISTORY" `
    -Title "ĐÓNG GÓI LỊCH SỬ DỰ ÁN (HISTORY ARCHIVED)" `
    -Description "Đã đóng gói toàn bộ nhật ký Plan, Process, Fix thành bản lưu trữ lịch sử lâu dài" `
    -ImpactedFiles @("docs/history/HISTORY_YYYYMMDD.md") `
    -Status "Hoàn tất thành công" `
    -ExtraNote "Thư mục docs được tối ưu hóa gọn gàng, sẵn sàng cho các đợt phát triển tiếp theo"
```
