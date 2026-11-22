param (
    [ValidateSet("patch", "minor", "major", "custom")]
    [string]$BumpType = "patch",

    [string]$CustomVersion = "",
    [string]$ReleaseTitle = "",
    [string]$ReleaseNotes = "",
    [switch]$DryRun,
    [switch]$SkipPush,
    [switch]$SkipTest,
    [switch]$BuildLocalApk
)

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")).Path
$buildGradlePath = Join-Path -Path $repoRoot -ChildPath "app\build.gradle.kts"

if (-not (Test-Path $buildGradlePath)) {
    Write-Error "Không tìm thấy app/build.gradle.kts tại: $buildGradlePath"
    exit 1
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   QUY TRÌNH PHÁT HÀNH PHIÊN BẢN MỚI      " -ForegroundColor Cyan
Write-Host "   VNPT PING TOOL - ZERO ADS EDITION      " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Đọc phiên bản hiện tại từ app/build.gradle.kts
$gradleContent = [System.IO.File]::ReadAllText($buildGradlePath, [System.Text.Encoding]::UTF8)

$codeMatch = [regex]::Match($gradleContent, 'versionCode\s*=\s*(\d+)')
$nameMatch = [regex]::Match($gradleContent, 'versionName\s*=\s*"([^"]+)"')

if (-not $codeMatch.Success -or -not $nameMatch.Success) {
    Write-Error "Không thể trích xuất versionCode hoặc versionName từ app/build.gradle.kts"
    exit 1
}

$currentCode = [int]$codeMatch.Groups[1].Value
$currentVersion = $nameMatch.Groups[1].Value
$newCode = $currentCode + 1

# 2. Tính toán phiên bản mới theo SemVer
$parts = $currentVersion.Split('.')
$major = if ($parts.Length -ge 1) { [int]$parts[0] } else { 1 }
$minor = if ($parts.Length -ge 2) { [int]$parts[1] } else { 0 }
$patch = if ($parts.Length -ge 3) { [int]$parts[2] } else { 0 }

$newVersion = ""
switch ($BumpType) {
    "patch" {
        $patch++
        $newVersion = "$major.$minor.$patch"
    }
    "minor" {
        $minor++
        $patch = 0
        $newVersion = "$major.$minor.$patch"
    }
    "major" {
        $major++
        $minor = 0
        $patch = 0
        $newVersion = "$major.$minor.$patch"
    }
    "custom" {
        if ([string]::IsNullOrWhiteSpace($CustomVersion)) {
            Write-Error "Vui lòng cung cấp -CustomVersion khi chọn BumpType = custom"
            exit 1
        }
        $newVersion = $CustomVersion.TrimStart('v', 'V')
    }
}

$tag = "v$newVersion"
Write-Host "Phiên bản hiện tại : $currentVersion (Build $currentCode)" -ForegroundColor Gray
Write-Host "Phiên bản mới      : $newVersion (Build $newCode)" -ForegroundColor Green
Write-Host "Git Tag phát hành  : $tag" -ForegroundColor Green

# 3. Kiểm tra biên dịch trước khi deploy
$gradlewBat = Join-Path -Path $repoRoot -ChildPath "gradlew.bat"
if (-not $SkipTest) {
    Write-Host "`n[1/6] Đang kiểm tra biên dịch (compileDebugKotlin)..." -ForegroundColor Yellow
    if (Test-Path $gradlewBat) {
        $testOutput = & $gradlewBat compileDebugKotlin 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Lỗi biên dịch Kotlin! Vui lòng sửa lỗi trước khi phát hành:`n$testOutput"
            exit 1
        }
        Write-Host "Biên dịch thành công! (BUILD SUCCESSFUL)" -ForegroundColor Green
    } else {
        Write-Warning "Không tìm thấy gradlew.bat, bỏ qua bước kiểm tra biên dịch."
    }
} else {
    Write-Host "`n[1/6] Bỏ qua bước kiểm tra biên dịch theo yêu cầu (-SkipTest)." -ForegroundColor Gray
}

# 4. Trích xuất Release Notes nếu chưa cung cấp
if ([string]::IsNullOrWhiteSpace($ReleaseNotes)) {
    Write-Host "`n[2/6] Đang trích xuất changelog từ commit gần nhất..." -ForegroundColor Yellow
    $lastTag = git describe --tags --abbrev=0 2>$null
    if ($lastTag) {
        $rawLog = git log "${lastTag}..HEAD" --oneline --no-merges 2>$null
    } else {
        $rawLog = git log -n 10 --oneline --no-merges 2>$null
    }
    
    if ($rawLog) {
        $ReleaseNotes = ($rawLog | ForEach-Object { "- $_" }) -join "`n"
    } else {
        $ReleaseNotes = "- Nâng cấp và cải thiện hiệu năng đo kiểm mạng`n- Tối ưu hóa giao diện Liquid Glass"
    }
}

if ([string]::IsNullOrWhiteSpace($ReleaseTitle)) {
    $ReleaseTitle = "Phiên bản $newVersion - VNPT Ping Tool"
}

Write-Host "Release Title: $ReleaseTitle" -ForegroundColor Gray

# 5. Cập nhật app/build.gradle.kts
Write-Host "`n[3/6] Đang cập nhật app/build.gradle.kts..." -ForegroundColor Yellow
$newGradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
$newGradleContent = $newGradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$newVersion`""

if ($DryRun) {
    Write-Host "[DRY RUN] Đã mô phỏng cập nhật phiên bản, không ghi đĩa." -ForegroundColor Magenta
    Write-Host "Nội dung Release Notes dự kiến:`n$ReleaseNotes" -ForegroundColor Gray
    exit 0
}

[System.IO.File]::WriteAllText($buildGradlePath, $newGradleContent, [System.Text.Encoding]::UTF8)
Write-Host "Đã cập nhật versionCode = $newCode và versionName = `"$newVersion`"" -ForegroundColor Green

# 6. Đóng gói Release APK có chữ ký số cục bộ
Write-Host "`n[4/6] Đang đóng gói Release APK có chữ ký số (assembleRelease)..." -ForegroundColor Yellow
$buildOutput = & $gradlewBat assembleRelease 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "Lỗi khi build APK release:`n$buildOutput"
    exit 1
}

$releaseApkPath = Join-Path -Path $repoRoot -ChildPath "app\build\outputs\apk\release\app-release.apk"
$artifactsDir = Join-Path -Path $repoRoot -ChildPath "release-artifacts"
if (-not (Test-Path $artifactsDir)) {
    New-Item -ItemType Directory -Path $artifactsDir -Force | Out-Null
}
$targetApkPath = Join-Path -Path $artifactsDir -ChildPath "PingTool-$tag.apk"
Copy-Item -Path $releaseApkPath -Destination $targetApkPath -Force
Write-Host "Đã tạo file APK release thành công: $targetApkPath" -ForegroundColor Green

# 7. Tạo Git Commit & Annotated Tag
Write-Host "`n[5/6] Đang tạo Git Commit và Tag $tag..." -ForegroundColor Yellow
git add app/build.gradle.kts
$commitMsg = "chore(release): bump version to $tag`n`n$ReleaseTitle`n`n$ReleaseNotes"
git commit -m $commitMsg

$tagAnnotation = "$ReleaseTitle`n`n$ReleaseNotes"
git tag -a $tag -m $tagAnnotation -f
Write-Host "Đã tạo commit và tag $tag thành công!" -ForegroundColor Green

# 8. Đẩy lên GitHub & Tải trực tiếp APK lên Releases
if (-not $SkipPush) {
    Write-Host "`n[6/6] Đang đẩy Commit và Tag lên GitHub origin..." -ForegroundColor Yellow
    git push origin HEAD
    git push origin $tag --force
    Write-Host "Đã đẩy tag $tag thành công lên GitHub!" -ForegroundColor Green

    # Kiểm tra gh CLI để tải trực tiếp APK
    $ghAvailable = Get-Command gh -ErrorAction SilentlyContinue
    if ($ghAvailable) {
        Write-Host "Đang đẩy trực tiếp file APK $targetApkPath lên GitHub Release qua gh CLI..." -ForegroundColor Yellow
        gh release create $tag $targetApkPath --title $ReleaseTitle --notes $ReleaseNotes 2>$null
        if ($LASTEXITCODE -ne 0) {
            # Nếu release đã tồn tại, upload clobber
            gh release upload $tag $targetApkPath --clobber
            gh release edit $tag --title $ReleaseTitle --notes $ReleaseNotes
        }
        Write-Host "Đã tải file APK lên GitHub Release thành công!" -ForegroundColor Green
    }
    
    Write-Host "`n🎉 Bản phát hành đã sẵn sàng tại:" -ForegroundColor Cyan
    Write-Host "👉 https://github.com/khuongnv/ping/releases/tag/$tag" -ForegroundColor Cyan
} else {
    Write-Host "`n[6/6] Bỏ qua bước đẩy lên GitHub theo yêu cầu (-SkipPush)." -ForegroundColor Yellow
    Write-Host "Bạn có thể tự đẩy thủ công bằng lệnh: git push origin HEAD; git push origin $tag" -ForegroundColor Gray
}

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "   PHÁT HÀNH $tag HOÀN TẤT THÀNH CÔNG!     " -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
