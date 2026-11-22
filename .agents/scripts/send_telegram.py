#!/usr/bin/env python3
"""
Script gửi thông báo Telegram cho dự án VNPT Ping Tool.
Tương thích chéo nền tảng (Windows/Linux/macOS), đọc cấu hình từ docs/.telegram.
"""

import os
import sys
import json
import urllib.request
import urllib.parse
from datetime import datetime
from pathlib import Path

def escape_html(text: str) -> str:
    if not text:
        return ""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

def load_telegram_config():
    bot_token = os.environ.get("TELEGRAM_BOT_TOKEN")
    channel_id = os.environ.get("TELEGRAM_CHANNEL_ID")
    
    script_dir = Path(__file__).resolve().parent
    potential_paths = [
        script_dir.parent.parent / "docs" / ".telegram",
        Path.cwd() / "docs" / ".telegram",
        Path.cwd() / ".telegram"
    ]
    
    for config_path in potential_paths:
        if config_path.exists():
            with open(config_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        k, v = line.split("=", 1)
                        k = k.strip()
                        v = v.strip()
                        if k == "TELEGRAM_BOT_TOKEN" and not bot_token:
                            bot_token = v
                        elif k == "TELEGRAM_CHANNEL_ID" and not channel_id:
                            channel_id = v
            break
            
    return bot_token, channel_id

def send_message(action: str, task_name: str = "Chung", title: str = "",
                 description: str = "", branch: str = "main", status: str = "",
                 details: list = None, impacted_files: list = None, extra_note: str = ""):
    bot_token, channel_id = load_telegram_config()
    if not bot_token or not channel_id:
        print("[Telegram Error] Không tìm thấy TELEGRAM_BOT_TOKEN hoặc TELEGRAM_CHANNEL_ID!", file=sys.stderr)
        return False

    action_map = {
        "TASK_START": ("🚀", "BẮT ĐẦU TASK MỚI", "🟢 Đang thực hiện"),
        "TASK_FINISH": ("✅", "HOÀN THÀNH TASK", "🔵 Hoàn tất thành công"),
        "PLAN_START": ("📋", "BẮT ĐẦU LẬP KẾ HOẠCH (PLAN)", "🟡 Đang phân tích & thiết kế"),
        "PLAN_FINISH": ("📑", "HOÀN TẤT KẾ HOẠCH (PLAN)", "🟠 Chờ người dùng phê duyệt"),
        "PROCESS_START": ("⚙️", "BẮT ĐẦU THỰC THI (PROCESS)", "🟢 Đang chỉnh sửa mã nguồn"),
        "PROCESS_FINISH": ("📦", "HOÀN TẤT THỰC THI (PROCESS)", "🔵 Đã cập nhật code & ghi log"),
        "FIX_START": ("🔍", "BẮT ĐẦU VÁ LỖI (BUG FIX)", "🔴 Đang điều tra nguyên nhân"),
        "FIX_FINISH": ("🩹", "HOÀN TẤT VÁ LỖI (BUG FIX)", "🔵 Đã khắc phục & ghi log fix"),
        "GIT_SYNC": ("🌿", "ĐỒNG BỘ MÃ NGUỒN (GIT SYNC)", "🔵 Đã commit & push"),
        "BUILD_START": ("🔨", "BẮT ĐẦU BIÊN DỊCH (BUILD/TEST)", "🟡 Đang biên dịch Android APK"),
        "BUILD_FINISH": ("🎯", "HOÀN TẤT BIÊN DỊCH (BUILD/TEST)", "🔵 Biên dịch thành công"),
        "CUSTOM": ("📢", "THÔNG BÁO HOẠT ĐỘNG", "ℹ️ Thông tin")
    }

    icon, default_title, default_status = action_map.get(action, ("🔔", "THÔNG BÁO HOẠT ĐỘNG", "ℹ️ Thông tin"))
    display_title = title if title else default_title
    display_status = status if status else default_status
    timestamp = datetime.now().strftime("%H:%M:%S %d/%m/%Y")

    lines = [
        f"{icon} <b>[VNPT PING TOOL] - {escape_html(display_title)}</b>",
        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        f"🏷 <b>Công việc:</b> <code>{escape_html(task_name)}</code>",
        f"🌿 <b>Nhánh Git:</b> <code>{escape_html(branch)}</code>",
        f"⏰ <b>Thời gian:</b> <code>{timestamp}</code>",
        f"📊 <b>Trạng thái:</b> <b>{escape_html(display_status)}</b>"
    ]

    if description:
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("📝 <b>Mô tả nội dung:</b>")
        lines.append(escape_html(description))

    if details:
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("📌 <b>Chi tiết thực hiện:</b>")
        for item in details:
            if item.strip():
                lines.append(f"• {escape_html(item.strip())}")

    if impacted_files:
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("📁 <b>Tệp tin liên quan / tạo mới:</b>")
        for f in impacted_files:
            if f.strip():
                lines.append(f"• <code>{escape_html(f.strip())}</code>")

    if extra_note:
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("💡 <b>Ghi chú / Bước tiếp theo:</b>")
        lines.append(f"<i>{escape_html(extra_note)}</i>")

    lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    lines.append("🤖 <i>Thông báo tự động từ Antigravity AI Agent</i>")

    message_text = "\n".join(lines)

    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    payload = json.dumps({
        "chat_id": channel_id,
        "text": message_text,
        "parse_mode": "HTML",
        "disable_web_page_preview": True
    }).encode("utf-8")

    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json; charset=utf-8"})
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if data.get("ok"):
                print(f"✅ [Telegram] Đã gửi thông báo thành công (Message ID: {data['result']['message_id']})")
                return True
            else:
                print(f"⚠️ [Telegram] Gửi không thành công: {data}", file=sys.stderr)
                return False
    except Exception as e:
        print(f"⚠️ [Telegram] Lỗi khi gửi: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Gửi thông báo Telegram cho VNPT Ping Tool")
    parser.add_argument("--action", required=True, help="Hành động (TASK_START, TASK_FINISH, ...)")
    parser.add_argument("--task-name", default="Chung", help="Tên công việc / Issue")
    parser.add_argument("--title", default="", help="Tiêu đề tùy chỉnh")
    parser.add_argument("--description", default="", help="Mô tả công việc")
    parser.add_argument("--branch", default="", help="Nhánh Git")
    parser.add_argument("--status", default="", help="Trạng thái")
    parser.add_argument("--details", nargs="*", default=[], help="Danh sách chi tiết")
    parser.add_argument("--files", nargs="*", default=[], help="Danh sách file")
    parser.add_argument("--extra-note", default="", help="Ghi chú thêm")

    args = parser.parse_args()
    send_message(
        action=args.action,
        task_name=args.task_name,
        title=args.title,
        description=args.description,
        branch=args.branch,
        status=args.status,
        details=args.details,
        impacted_files=args.files,
        extra_note=args.extra_note
    )
