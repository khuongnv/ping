# 🚀 VNPT Ping Tool (Zero Ads - Liquid Glass Edition)

Ứng dụng Android thực hiện nhiệm vụ đo kiểm tra kết nối mạng (Ping & Traceroute), kiểm tra IP mạng nội bộ và công cộng với phong cách giao diện **Liquid Glass x Apple Music HIG** hiện đại, sang trọng, loại bỏ hoàn toàn 100% quảng cáo gây phiền toái.

---

## ✨ Tính Năng Nổi Bật

1. **Ping Engine Real-time**:
   - Nhập Host / IP tùy ý hoặc chọn nhanh (8.8.8.8, 1.1.1.1, vnpt.com.vn...).
   - Bắt đầu / Dừng Ping với hiệu ứng đàn hồi Spring Physics.
   - **Biểu đồ sóng Realtime Canvas** mượt mà 60/120 FPS, đổi màu trực quan theo độ trễ (Xanh < 50ms, Vàng 50-150ms, Đỏ > 150ms hoặc mất gói).
   - Bộ chỉ số RTT độ trễ: Trung bình (Avg), Tối đa (Max), Tối thiểu (Min).
   - Bộ chỉ số Gói tin: Đã gửi (Sent), Đã nhận (Received), Tỉ lệ mất gói (% Loss).
   - **Terminal Live Log**: Nhật ký phản hồi ICMP thời gian thực chuẩn Linux/Android.
2. **Traceroute Chuyên Nghiệp**:
   - Dò từng chặng mạng (hop) và hiển thị độ trễ từng trạm trung chuyển tới đích.
3. **Tra cứu IP của tôi (My IP Info)**:
   - **Mạng nội bộ**: IP Wi-Fi, SSID, Default Gateway, Mặt nạ mạng (Subnet Mask), DNS Server.
   - **Internet**: Public IP, Nhà mạng cung cấp (ISP), Quốc gia (Vietnam).
   - Chạm để sao chép nhanh bất kỳ chỉ số nào vào Clipboard.
4. **Lịch sử đo kiểm (History)**:
   - Tự động lưu vết các phiên Ping, RTT và % Loss.
   - Chạm vào lịch sử để thực hiện Ping lại nhanh.
5. **Cài đặt & Tùy biến**:
   - Tùy chỉnh Kích thước gói tin (bytes), Khoảng cách gửi (Interval ms), Thời gian chờ (Timeout).
6. **Cơ Chế Tự Động Cập Nhật Ứng Dụng (In-app Update via GitHub Releases)**:
   - Ứng dụng tự động kết nối với GitHub API: `https://api.github.com/repos/khuongnv/ping/releases/latest`.
   - Khi có phiên bản mới hơn phiên bản hiện tại, màn hình Cài đặt sẽ hiển thị thông báo bản cập nhật mới kèm changelog và nút **Tải & Cài đặt APK** tự động thông qua `DownloadManager` và `FileProvider`.

---

## 🎨 Chuẩn UI/UX Liquid Glass

- Nền AMOLED đen sâu `#000000`.
- Thẻ giao diện bán trong suốt viền phản quang `0.75dp Luminous Rim`.
- Dock điều hướng đáy nổi `FloatingGlassBottomBar` với viên thuốc Capsule Pill.
- Phản hồi chuyển động `Spring Physics` đàn hồi tự nhiên.

---

## 🛠️ Hướng Dẫn Phát Hành Bản Cập Nhật Mới (Release)

Khi bạn muốn phát hành bản cập nhật cho ứng dụng:

1. Đẩy mã nguồn lên branch `main` hoặc tạo Git Tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
2. GitHub Action (`.github/workflows/release.yml`) sẽ tự động:
   - Biên dịch file APK hoàn chỉnh.
   - Tạo bản Release trên GitHub `https://github.com/khuongnv/ping/releases`.
   - Đính kèm file `PingTool-v1.0.1.apk`.
3. Tất cả người dùng đang cài ứng dụng trên điện thoại khi vào mục **Cài đặt** (hoặc mở app) sẽ tự động nhận diện có bản cập nhật mới và có thể bấm cập nhật ngay trong app.
