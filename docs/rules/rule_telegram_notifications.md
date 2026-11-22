# 📢 QUY TẮC THÔNG BÁO TELEGRAM CHO MỌI HOẠT ĐỘNG AGENT
## Dự án: VNPT Ping Tool (`com.vnpt.pingtool`)

---

## 1. NGUYÊN TẮC BẮT BUỘC (MANDATORY RULE)

> **TẤT CẢ CÁC THAO TÁC BẮT ĐẦU VÀ KẾT THÚC CỦA MỌI TASK ĐỀU PHẢI GỬI THÔNG BÁO TIẾNG VIỆT VỀ KÊNH TELEGRAM.**

Không có ngoại lệ:
- Bắt đầu 1 task mới (`issue-start`) -> **Gửi thông báo bắt đầu**.
- Kết thúc 1 task (`issue-finish`) -> **Gửi thông báo kết thúc**.
- Bắt đầu lập kế hoạch (`plan-docs`) -> **Gửi thông báo bắt đầu Plan**.
- Hoàn tất lập kế hoạch (`plan-docs`) -> **Gửi thông báo hoàn tất Plan & chờ duyệt**.
- Bắt đầu thực thi code (`process-log`) -> **Gửi thông báo bắt đầu thực thi**.
- Hoàn tất thực thi code (`process-log`) -> **Gửi thông báo hoàn tất & báo cáo file**.
- Bắt đầu vá lỗi (`fix-log`) -> **Gửi thông báo bắt đầu Fix**.
- Hoàn tất vá lỗi (`fix-log`) -> **Gửi thông báo đã vá lỗi xong**.
- Đồng bộ mã nguồn (`git-sync`) -> **Gửi thông báo commit & push**.
- Biên dịch ứng dụng (`android-build`) -> **Gửi thông báo kết quả build**.

---

## 2. THÔNG TIN KẾT NỐI TELEGRAM

Thông tin kết nối được lưu trong `docs/.telegram`:
- **Bot Name:** HNiBOT (`@VnptHniBot`)
- **Token:** Đọc từ key `TELEGRAM_BOT_TOKEN`
- **Channel ID:** Đọc từ key `TELEGRAM_CHANNEL_ID`

---

## 3. CÔNG CỤ THỰC HIỆN GỬI THÔNG BÁO

Hệ thống cung cấp sẵn 2 kịch bản gửi thông báo:

### Cách 1: PowerShell Script (Khuyến nghị trên Windows):
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "TASK_START" `
    -TaskName "<Mã hoặc Tên Task>" `
    -Description "<Mô tả công việc tiếng Việt>" `
    -Details @("<Ý 1>", "<Ý 2>") `
    -ImpactedFiles @("<file 1>", "<file 2>") `
    -ExtraNote "<Ghi chú tiếp theo>"
```

### Cách 2: Python Script (Cross-platform):
```bash
python .agents/scripts/send_telegram.py \
    --action TASK_START \
    --task-name "<Mã Task>" \
    --description "<Mô tả>" \
    --details "<Ý 1>" "<Ý 2>" \
    --files "<file 1>" "<file 2>"
```

---

## 4. CÁC LOẠI HÀNH ĐỘNG VÀ MẪU HIỂN THỊ

| Mã Action | Biểu tượng & Tiêu đề | Trạng thái hiển thị | Khi nào sử dụng? |
|:---|:---|:---|:---|
| `TASK_START` | 🚀 **BẮT ĐẦU TASK MỚI** | 🟢 Đang thực hiện | Khi chạy `issue-start` hoặc bắt đầu một yêu cầu công việc mới |
| `TASK_FINISH` | ✅ **HOÀN THÀNH TASK** | 🔵 Hoàn tất thành công | Khi chạy `issue-finish` hoặc kết thúc hoàn toàn một yêu cầu |
| `PLAN_START` | 📋 **BẮT ĐẦU LẬP KẾ HOẠCH** | 🟡 Đang phân tích & thiết kế | Khi nhận lệnh `/plan`, bắt đầu nghiên cứu giải pháp |
| `PLAN_FINISH` | 📑 **HOÀN TẤT KẾ HOẠCH** | 🟠 Chờ người dùng phê duyệt | Khi sinh xong các file plan trong `docs/plans/` |
| `PROCESS_START` | ⚙️ **BẮT ĐẦU THỰC THI** | 🟢 Đang chỉnh sửa mã nguồn | Khi bắt đầu sửa code sau khi plan được duyệt |
| `PROCESS_FINISH` | 📦 **HOÀN TẤT THỰC THI** | 🔵 Đã cập nhật code & ghi log | Khi hoàn thành code và tạo file nhật ký `docs/processes/` |
| `FIX_START` | 🔍 **BẮT ĐẦU VÁ LỖI** | 🔴 Đang điều tra nguyên nhân | Khi phát hiện bug phát sinh hoặc nhận lệnh `/fix` |
| `FIX_FINISH` | 🩹 **HOÀN TẤT VÁ LỖI** | 🔵 Đã khắc phục & ghi log fix | Khi đã sửa xong bug và tạo log `docs/fixes/` |
| `GIT_SYNC` | 🌿 **ĐỒNG BỘ MÃ NGUỒN** | 🔵 Đã commit & push | Khi thực hiện commit và push code lên remote |
| `BUILD_START` | 🔨 **BẮT ĐẦU BIÊN DỊCH** | 🟡 Đang biên dịch Android APK | Khi kích hoạt kiểm tra build Android |
| `BUILD_FINISH` | 🎯 **HOÀN TẤT BIÊN DỊCH** | 🔵 Biên dịch thành công | Khi build APK hoặc chạy test thành công |

---

## 5. TIÊU CHUẨN NỘI DUNG THÔNG BÁO

1. **Ngôn ngữ:** 100% tiếng Việt chuẩn mực kỹ thuật, chuyên nghiệp, súc tích.
2. **Cấu trúc:** Có tiêu đề nổi bật, mã task, nhánh git, mốc thời gian chính xác, trạng thái có mã màu emoji, tóm tắt rõ ràng các đầu việc và danh sách file thay đổi.
3. **Định dạng:** Sử dụng chuẩn HTML của Telegram (`<b>`, `<code>`, `<i>`), tự động escape ký tự đặc biệt để không bao giờ bị lỗi cú pháp API.