# 📋 Nhật Ký Thực Thi: Sửa triệt để logic thẩm định mật khẩu và chấm dứt lỗi nhập gì cũng báo khớp

- **Ngày thực hiện**: 2026-11-22 17:59:18
- **Agent phụ trách**: `lead-architect`
- **Yêu cầu gốc**: Sửa triệt để logic thẩm định mật khẩu và chấm dứt lỗi nhập gì cũng báo khớp
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Kế Hoạch (Analysis & Planning)
- **Bối cảnh & Mục tiêu kỹ thuật**:
  - Người dùng phản ánh: "Phần kiểm tra mật khẩu ngáo rồi. Nhập là gì cũng báo khớp."
  - Nguyên nhân gốc rễ:
    1. Khi người dùng nhập bất kỳ mật khẩu nào vào ô thẩm định, engine quét từ điển đối chiếu `candidate.equals(targetPwd)`. Khi tìm thấy từ đó trong từ điển, hệ thống lại phát thông báo `🎯 ĐÃ PHÁT HIỆN MẬT KHẨU TRÙNG KHỚP CHO MẠNG WI-FI [SSID]!` và tự động lưu vào `_auditHistory` kèm nút kết nối và mã QR. Điều này khiến người dùng nhập bất cứ từ nào có trong từ điển thì app đều báo đó là mật khẩu của mạng Wi-Fi được chọn.
    2. Trong `WifiConnectionManager.probeWifiPassword`, điều kiện `if (COMMON_TEST_PASSWORDS.contains(candidate)) return true` tự động trả về `true` cho mọi candidate thuộc danh sách mẫu ngay khi máy có kết nối Wi-Fi.
    3. Việc tự động lưu chuỗi người dùng nhập vào `verified_pwd` của SSID gây sai lệch dữ liệu nhớ.
- **Giải pháp triệt để**:
  - Phân tách dứt khoát 2 chức năng:
    + **Thẩm định độ an toàn mật khẩu cá nhân**: Đánh giá entropy, mức độ bảo mật và kiểm tra xem mật khẩu có nằm trong từ điển phổ biến không. Tuyệt đối KHÔNG gán làm mật khẩu của mạng Wi-Fi, KHÔNG báo "đã tìm thấy mật khẩu cho mạng này", và KHÔNG lưu vào lịch sử khớp mật khẩu Wi-Fi.
    + **Quét SCAN từ điển cho mạng Wi-Fi (để trống ô nhập)**: Duyệt tuần tự từ điển cấp 1->99. Chỉ báo khớp NẾU thực sự xác thực được mật khẩu đã lưu trong hệ thống (`getSavedWifiPassword`: Root hoặc Android 8/9).
  - Loại bỏ hoàn toàn điều kiện giả định trúng khớp bừa bãi trong `probeWifiPassword`.

## 2. Chi Tiết Các Bước Thực Hiện (Execution Steps)
- **Bước 1**: Sửa `WifiConnectionManager.probeWifiPassword`: Chỉ trả về `true` khi `saved != null` và `candidate.equals(saved)`. Xóa bỏ hoàn toàn điều kiện trả về `true` giả lập.
- **Bước 2**: Sửa `PingStorage.kt`: Loại bỏ `getVerifiedPassword` và `saveVerifiedPassword` tự động lưu chuỗi nhập.
- **Bước 3**: Sửa `PingViewModel.kt`: Tách biệt cờ `isTargetPasswordAudit`. Khi người dùng tự nhập mật khẩu thẩm định, tuyệt đối không lưu vào `_auditHistory`.
- **Bước 4**: Sửa `WifiScanEngine.kt`: Tách biệt thông báo log và báo cáo tổng kết giữa thẩm định từ điển cá nhân và quét mạng Wi-Fi thực tế.
- **Bước 5**: Sửa `ScanScreen.kt`: Cập nhật nhãn rõ ràng, hướng dẫn trực quan và phân biệt hiển thị kết quả.
- **Bước 6**: Chạy `./gradlew test`: 100% test suites pass.

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/network/WifiConnectionManager.kt` - Chỉ xác thực theo cấu hình đã lưu thực tế
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/storage/PingStorage.kt` - Xóa bỏ lưu nhớ chuỗi gõ tự do
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/viewmodel/PingViewModel.kt` - Ngăn lưu lịch sử khớp khi tự thẩm định mật khẩu
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/engine/WifiScanEngine.kt` - Phân định rõ thông báo thẩm định từ điển và quét Wi-Fi
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/ui/screens/ScanScreen.kt` - Cập nhật nhãn và giao diện kết quả chính xác

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- Chạy `./gradlew test`: 49 actionable tasks, `BUILD SUCCESSFUL in 32s`.

## 5. Kết Luận & Hướng Dẫn Tiếp Theo (Conclusion & Next Steps)
- Đã chấm dứt hoàn toàn hiện tượng "nhập là gì cũng báo khớp".
- Sẵn sàng phát hành phiên bản `v1.4.4`.
