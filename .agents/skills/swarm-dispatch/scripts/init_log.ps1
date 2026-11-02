param (
    [Parameter(Mandatory=$true)]
    [string]$TaskSlug,

    [Parameter(Mandatory=$true)]
    [string]$TaskTitle,

    [Parameter(Mandatory=$false)]
    [string]$AgentName = "lead-architect"
)

$today = Get-Date -Format "yyyy-MM-dd"
$now = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$logFileName = "${today}_${TaskSlug}.md"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")).Path
$resolvedLogDir = Join-Path -Path $repoRoot -ChildPath "docs\logs"
$templatePath = Join-Path -Path $PSScriptRoot -ChildPath "..\resources\log_template.md"

if (-not (Test-Path $resolvedLogDir)) {
    New-Item -ItemType Directory -Path $resolvedLogDir -Force | Out-Null
}

$logFilePath = Join-Path -Path $resolvedLogDir -ChildPath $logFileName
$indexFilePath = Join-Path -Path $resolvedLogDir -ChildPath "README.md"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

if (Test-Path $logFilePath) {
    Write-Host "Tệp log đã tồn tại: $logFilePath" -ForegroundColor Yellow
} else {
    if (Test-Path $templatePath) {
        $rawTemplate = [System.IO.File]::ReadAllText($templatePath, [System.Text.Encoding]::UTF8)
        $template = $rawTemplate.Replace("{{TASK_TITLE}}", $TaskTitle).Replace("{{DATE_NOW}}", $now).Replace("{{AGENT_NAME}}", $AgentName)
    } else {
        $template = "# 📋 Nhật Ký Thực Thi: $TaskTitle`n`n- Ngày: $now`n- Agent: $AgentName`n- Trạng thái: Đang tiến hành`n"
    }
    [System.IO.File]::WriteAllText($logFilePath, $template, $utf8NoBom)
    Write-Host "Đã khởi tạo tệp log: $logFilePath" -ForegroundColor Green
}

# Khởi tạo README.md nếu chưa có
if (-not (Test-Path $indexFilePath)) {
    $initReadme = "# 📋 Chỉ Mục Nhật Ký Thực Thi Nhiệm Vụ (Task Execution Logs)`n`n| Ngày | Tệp Nhật Ký | Nhiệm Vụ | Agent Chủ Trì | Trạng Thái |`n| :--- | :--- | :--- | :--- | :--- |`n"
    [System.IO.File]::WriteAllText($indexFilePath, $initReadme, $utf8NoBom)
}

# Thêm bản ghi vào bảng README.md nếu chưa có
$content = [System.IO.File]::ReadAllText($indexFilePath, [System.Text.Encoding]::UTF8)
if ($content -notmatch [regex]::Escape($logFileName)) {
    $newRow = "| {0} | [`{1}`](./{1}) | {2} | `{3}` | 🔄 Đang tiến hành |`r`n" -f $today, $logFileName, $TaskTitle, $AgentName
    $pattern = "(\| :--- \| :--- \| :--- \| :--- \| :--- \|\r?\n)"
    if ($content -match $pattern) {
        $updatedContent = $content -replace $pattern, "`$1$newRow"
        [System.IO.File]::WriteAllText($indexFilePath, $updatedContent, $utf8NoBom)
        Write-Host "Đã cập nhật chỉ mục trong: $indexFilePath" -ForegroundColor Cyan
    }
}
