---
name: android-build
description: "Compile Android APK or run unit tests, and send build status notification to Telegram. Use when user says /build, kiem tra build, build apk, chay test."
user-invocable: true
---

# Android Build & Test

## Purpose
Run Gradle compilation (`assembleDebug`) or unit tests for the Android project, verify code integrity, and **send build status notification to Telegram**.

## Procedure

### Bước 1: GỬI THÔNG BÁO BẮT ĐẦU BUILD LÊN TELEGRAM
Chạy với `BypassSandbox: true`:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "BUILD_START" `
    -TaskName "ANDROID-BUILD-CHECK" `
    -Description "Bắt đầu quy trình kiểm tra biên dịch ứng dụng Android APK với Gradle" `
    -Details @("Kiểm tra cú pháp Kotlin & Compose Compiler", "Chạy Gradle assembleDebug")
```

### Bước 2: Thực hiện lệnh Build
Chạy trong terminal:
```bash
./gradlew assembleDebug
```
Hoặc chạy kiểm thử:
```bash
./gradlew testDebugUnitTest
```

### Bước 3: GỬI THÔNG BÁO KẾT QUẢ BUILD LÊN TELEGRAM
Nếu biên dịch thành công:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "BUILD_FINISH" `
    -TaskName "ANDROID-BUILD-CHECK" `
    -Description "Biên dịch ứng dụng Android thành công 100%, không phát sinh lỗi cú pháp hay tài nguyên" `
    -Status "🔵 Biên dịch thành công" `
    -Details @("Gradle BUILD SUCCESSFUL", "File APK sẵn sàng tại app/build/outputs/apk/debug/") `
    -ExtraNote "Ứng dụng sẵn sàng để cài đặt thử nghiệm hoặc tạo bản phát hành"
```

Nếu biên dịch thất bại:
```powershell
powershell -ExecutionPolicy Bypass -File .agents/scripts/send_telegram.ps1 `
    -Action "BUILD_FINISH" `
    -TaskName "ANDROID-BUILD-CHECK" `
    -Title "BIÊN DỊCH THẤT BẠI (BUILD FAILED)" `
    -Description "Quá trình biên dịch phát hiện lỗi trong mã nguồn hoặc tài nguyên Android" `
    -Status "🔴 Biên dịch thất bại" `
    -Details @("<tóm tắt lỗi từ Gradle output>") `
    -ExtraNote "AI Agent đang tiến hành phân tích lỗi để sửa chữa (sử dụng /fix)"
```

### Bước 4: Báo cáo cho người dùng
Báo cáo kết quả chi tiết từ Gradle terminal cho người dùng.
