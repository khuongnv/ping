<#
.SYNOPSIS
    Gửi thông báo hoạt động của AI Agent và tác vụ dự án VNPT Ping Tool về kênh Telegram.
.DESCRIPTION
    Script tự động đọc cấu hình token và channel ID từ docs/.telegram, chuẩn hóa nội dung tiếng Việt
    chuyên nghiệp, hỗ trợ đầy đủ các giai đoạn vòng đời (Task Start/Finish, Plan, Process, Fix, Git Sync, Build).
.PARAMETER Action
    Loại hành động: TASK_START, TASK_FINISH, PLAN_START, PLAN_FINISH, PROCESS_START, PROCESS_FINISH, FIX_START, FIX_FINISH, GIT_SYNC, BUILD_START, BUILD_FINISH, CUSTOM
.PARAMETER TaskName
    Mã hoặc tên công việc / issue (ví dụ: HNI-101-ping-chart-opt, FEATURE-TRACEROUTE)
.PARAMETER Title
    Tiêu đề hiển thị (tùy chọn, tự động sinh theo Action nếu bỏ trống)
.PARAMETER Description
    Mô tả ngắn gọn về công việc hoặc kết quả đạt được
.PARAMETER Branch
    Tên nhánh Git (tự động nhận diện nếu bỏ trống)
.PARAMETER Status
    Trạng thái công việc: Đang thực hiện, Thành công, Cảnh báo, Lỗi, Chờ phê duyệt...
.PARAMETER Details
    Mảng chuỗi hoặc danh sách các điểm chi tiết của công việc
.PARAMETER ImpactedFiles
    Mảng chuỗi hoặc danh sách các file được tạo mới / chỉnh sửa
.PARAMETER ExtraNote
    Ghi chú bổ sung hoặc nhắc nhở hành động tiếp theo
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "TASK_START", "TASK_FINISH",
        "PLAN_START", "PLAN_FINISH",
        "PROCESS_START", "PROCESS_FINISH",
        "FIX_START", "FIX_FINISH",
        "GIT_SYNC",
        "BUILD_START", "BUILD_FINISH",
        "CUSTOM"
    )]
    [string]$Action,

    [Parameter(Mandatory = $false)]
    [string]$TaskName = "Chung",

    [Parameter(Mandatory = $false)]
    [string]$Title = "",

    [Parameter(Mandatory = $false)]
    [string]$Description = "",

    [Parameter(Mandatory = $false)]
    [string]$Branch = "",

    [Parameter(Mandatory = $false)]
    [string]$Status = "",

    [Parameter(Mandatory = $false)]
    [string[]]$Details = @(),

    [Parameter(Mandatory = $false)]
    [string[]]$ImpactedFiles = @(),

    [Parameter(Mandatory = $false)]
    [string]$ExtraNote = "",

    [Parameter(Mandatory = $false)]
    [string]$ConfigFile = ""
)

function Escape-Html([string]$text) {
    if ([string]::IsNullOrEmpty($text)) { return "" }
    return $text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
}

# 1. Tìm và đọc file cấu hình Telegram (.telegram)
$configPath = $ConfigFile
if ([string]::IsNullOrEmpty($configPath)) {
    $potentialPaths = @(
        (Join-Path $PSScriptRoot "../../docs/.telegram"),
        (Join-Path $PWD "docs/.telegram"),
        (Join-Path $PWD ".telegram")
    )
    foreach ($p in $potentialPaths) {
        if (Test-Path $p) {
            $configPath = (Resolve-Path $p).Path
            break
        }
    }
}

$botToken = $env:TELEGRAM_BOT_TOKEN
$channelId = $env:TELEGRAM_CHANNEL_ID

if (Test-Path $configPath) {
    Get-Content $configPath | ForEach-Object {
        $line = $_.Trim()
        if (-not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $val = $parts[1].Trim()
            if ($key -eq "TELEGRAM_BOT_TOKEN" -and [string]::IsNullOrEmpty($botToken)) {
                $botToken = $val
            }
            if ($key -eq "TELEGRAM_CHANNEL_ID" -and [string]::IsNullOrEmpty($channelId)) {
                $channelId = $val
            }
        }
    }
}

if ([string]::IsNullOrEmpty($botToken) -or [string]::IsNullOrEmpty($channelId)) {
    Write-Error "Không tìm thấy TELEGRAM_BOT_TOKEN hoặc TELEGRAM_CHANNEL_ID trong docs/.telegram hoặc biến môi trường!"
    exit 1
}

# 2. Tự động nhận diện Git Branch nếu không truyền
if ([string]::IsNullOrEmpty($Branch)) {
    try {
        $gitBranch = git branch --show-current 2>$null
        if ($gitBranch) {
            $Branch = $gitBranch.Trim()
        } else {
            $Branch = "main"
        }
    } catch {
        $Branch = "main"
    }
}

# 3. Chuẩn hóa Tiêu đề, Icon và Trạng thái mặc định theo Action
$actionIcon = "🔔"
$actionTitle = ""
$defaultStatus = ""

switch ($Action) {
    "TASK_START" {
        $actionIcon = "🚀"
        $actionTitle = "BẮT ĐẦU TASK MỚI"
        $defaultStatus = "🟢 Đang thực hiện"
    }
    "TASK_FINISH" {
        $actionIcon = "✅"
        $actionTitle = "HOÀN THÀNH TASK"
        $defaultStatus = "🔵 Hoàn tất thành công"
    }
    "PLAN_START" {
        $actionIcon = "📋"
        $actionTitle = "BẮT ĐẦU LẬP KẾ HOẠCH (PLAN)"
        $defaultStatus = "🟡 Đang phân tích & thiết kế"
    }
    "PLAN_FINISH" {
        $actionIcon = "📑"
        $actionTitle = "HOÀN TẤT KẾ HOẠCH (PLAN)"
        $defaultStatus = "🟠 Chờ người dùng phê duyệt"
    }
    "PROCESS_START" {
        $actionIcon = "⚙️"
        $actionTitle = "BẮT ĐẦU THỰC THI (PROCESS)"
        $defaultStatus = "🟢 Đang chỉnh sửa mã nguồn"
    }
    "PROCESS_FINISH" {
        $actionIcon = "📦"
        $actionTitle = "HOÀN TẤT THỰC THI (PROCESS)"
        $defaultStatus = "🔵 Đã cập nhật code & ghi log"
    }
    "FIX_START" {
        $actionIcon = "🔍"
        $actionTitle = "BẮT ĐẦU VÁ LỖI (BUG FIX)"
        $defaultStatus = "🔴 Đang điều tra nguyên nhân"
    }
    "FIX_FINISH" {
        $actionIcon = "🩹"
        $actionTitle = "HOÀN TẤT VÁ LỖI (BUG FIX)"
        $defaultStatus = "🔵 Đã khắc phục & ghi log fix"
    }
    "GIT_SYNC" {
        $actionIcon = "🌿"
        $actionTitle = "ĐỒNG BỘ MÃ NGUỒN (GIT SYNC)"
        $defaultStatus = "🔵 Đã commit & push"
    }
    "BUILD_START" {
        $actionIcon = "🔨"
        $actionTitle = "BẮT ĐẦU BIÊN DỊCH (BUILD/TEST)"
        $defaultStatus = "🟡 Đang biên dịch Android APK"
    }
    "BUILD_FINISH" {
        $actionIcon = "🎯"
        $actionTitle = "HOÀN TẤT BIÊN DỊCH (BUILD/TEST)"
        $defaultStatus = "🔵 Biên dịch thành công"
    }
    "CUSTOM" {
        $actionIcon = "📢"
        $actionTitle = "THÔNG BÁO HOẠT ĐỘNG"
        $defaultStatus = "ℹ️ Thông tin"
    }
}

if (-not [string]::IsNullOrEmpty($Title)) {
    $actionTitle = $Title
}
if ([string]::IsNullOrEmpty($Status)) {
    $Status = $defaultStatus
}

# 4. Định dạng thời gian
$currentTime = Get-Date -Format "HH:mm:ss dd/MM/yyyy"

# 5. Xây dựng nội dung tin nhắn HTML chuyên nghiệp
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("$actionIcon <b>[VNPT PING TOOL] - $actionTitle</b>")
[void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
[void]$sb.AppendLine("🏷 <b>Công việc:</b> <code>$(Escape-Html $TaskName)</code>")
[void]$sb.AppendLine("🌿 <b>Nhánh Git:</b> <code>$(Escape-Html $Branch)</code>")
[void]$sb.AppendLine("⏰ <b>Thời gian:</b> <code>$currentTime</code>")
[void]$sb.AppendLine("📊 <b>Trạng thái:</b> <b>$(Escape-Html $Status)</b>")

if (-not [string]::IsNullOrEmpty($Description)) {
    [void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    [void]$sb.AppendLine("📝 <b>Mô tả nội dung:</b>")
    [void]$sb.AppendLine("$(Escape-Html $Description)")
}

if ($Details -and $Details.Count -gt 0) {
    [void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    [void]$sb.AppendLine("📌 <b>Chi tiết thực hiện:</b>")
    foreach ($item in $Details) {
        if (-not [string]::IsNullOrWhiteSpace($item)) {
            [void]$sb.AppendLine("• $(Escape-Html $item)")
        }
    }
}

if ($ImpactedFiles -and $ImpactedFiles.Count -gt 0) {
    [void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    [void]$sb.AppendLine("📁 <b>Tệp tin liên quan / tạo mới:</b>")
    foreach ($f in $ImpactedFiles) {
        if (-not [string]::IsNullOrWhiteSpace($f)) {
            [void]$sb.AppendLine("• <code>$(Escape-Html $f)</code>")
        }
    }
}

if (-not [string]::IsNullOrEmpty($ExtraNote)) {
    [void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    [void]$sb.AppendLine("💡 <b>Ghi chú / Bước tiếp theo:</b>")
    [void]$sb.AppendLine("<i>$(Escape-Html $ExtraNote)</i>")
}

[void]$sb.AppendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
[void]$sb.AppendLine("🤖 <i>Thông báo tự động từ Antigravity AI Agent</i>")

$finalHtml = $sb.ToString()

# 6. Gửi request tới Telegram Bot API
$apiUrl = "https://api.telegram.org/bot$botToken/sendMessage"
$payload = @{
    chat_id = $channelId
    text = $finalHtml
    parse_mode = "HTML"
    disable_web_page_preview = $true
} | ConvertTo-Json -Compress

try {
    $response = Invoke-RestMethod -Uri $apiUrl -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($payload)) -ContentType "application/json; charset=utf-8"
    if ($response.ok) {
        Write-Host "✅ [Telegram] Đã gửi thông báo thành công (Message ID: $($response.result.message_id))" -ForegroundColor Green
    } else {
        Write-Warning "⚠️ [Telegram] Gửi không thành công: $($response | ConvertTo-Json -Depth 3)"
    }
} catch {
    Write-Warning "⚠️ [Telegram] Ngoại lệ khi gửi tin nhắn: $($_.Exception.Message)"
}
