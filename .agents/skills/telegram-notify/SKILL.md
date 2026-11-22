---
name: telegram-notify
description: "Send professional Vietnamese notifications to the Telegram channel. Use when user says /notify, /telegram, gui thong bao telegram, bao cao tien do."
user-invocable: true
---

# Telegram Notify

## Purpose
Send structured, professional Vietnamese status updates directly to the project's Telegram channel (`HNiBOT`).

## Credentials
Credentials are automatically read from `docs/.telegram`:
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHANNEL_ID`

## Execution
Run the helper script outside sandbox (`BypassSandbox: true`):
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "CUSTOM" `
    -TaskName "<Tên công việc>" `
    -Title "<Tiêu đề thông báo>" `
    -Description "<Nội dung mô tả tiếng Việt>" `
    -Status "Thông tin" `
    -Details @("<Điểm chính 1>", "<Điểm chính 2>") `
    -ImpactedFiles @("<file 1>", "<file 2>") `
    -ExtraNote "<Ghi chú thêm>"
```

Or via Python:
```bash
python .agents/scripts/send_telegram.py --action CUSTOM --task-name "<Tên Task>" --title "<Tiêu đề>" --description "<Mô tả>"
```
