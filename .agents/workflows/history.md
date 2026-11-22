---
description: Đóng gói toàn bộ nhật ký Plan, Process, Fix thành một bản History lưu trữ lâu dài và dọn dẹp thư mục docs
---

# Mục đích
Gom toàn bộ các file rải rác trong `docs/plans/`, `docs/processes/`, `docs/fixes/` thành 1 file duy nhất `docs/history/HISTORY_YYYYMMDD.md` để giữ dự án luôn gọn gàng và dễ theo dõi.

# Các bước thực thi:
1. Quét toàn bộ các file `.md` trong 3 thư mục `docs/plans/`, `docs/processes/`, `docs/fixes/`.
2. Đọc nội dung và tổng hợp có cấu trúc thành `docs/history/HISTORY_YYYYMMDD.md`.
3. **HỎI Ý KIẾN NGƯỜI DÙNG** trước khi xóa các file nguồn con (chỉ xóa file markdown con, tuyệt đối không xóa thư mục hay file `.gitkeep`).
4. **Gửi thông báo Telegram XÁC NHẬN ĐÓNG GÓI:**
   ```powershell
   powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 -Action "CUSTOM" -TaskName "ARCHIVE-HISTORY" -Title "ĐÓNG GÓI LỊCH SỬ DỰ ÁN" -Description "Đã tổng hợp toàn bộ Plan, Process, Fix vào docs/history/HISTORY_YYYYMMDD.md" -ImpactedFiles @("docs/history/HISTORY_YYYYMMDD.md")
   ```
5. Báo cáo trạng thái lưu trữ cho người dùng.
