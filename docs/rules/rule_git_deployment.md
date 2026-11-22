# 🌿 QUY TẮC QUẢN LÝ GIT BRANCH & PHÁT HÀNH
## Dự án: VNPT Ping Tool (`com.vnpt.pingtool`)

> **Kế thừa và quy chuẩn hóa từ:** `docs/GIT_BRANCH_STRATEGY.md`

---

## 1. MÔ HÌNH NHÁNH (GIT BRANCHING MODEL)

| Nhánh | Mục đích | Nguồn tạo | Merge về | Quy định bảo vệ |
|:---|:---|:---|:---|:---|
| `main` | Bản phát hành chính thức (Production). Mỗi release tạo Git Tag `vX.Y.Z`. | Khởi tạo repo | Nhận từ `release/*`, `hotfix/*` | Protected. Cấm push trực tiếp. |
| `develop` | Tích hợp các tính năng đang phát triển. | `main` | Nhận từ `feature/*`, `bugfix/*` | Protected. Merge qua PR/MR hoặc commit có kiểm duyệt. |
| `feature/*` | Phát triển tính năng mới (ví dụ: `feature/ping-chart-opt`, `feature/HNI-101-traceroute`). | `develop` | `develop` | Xóa nhánh local sau khi merge. |
| `bugfix/*` | Sửa lỗi trong quá trình phát triển (ví dụ: `bugfix/ip-lookup-timeout`). | `develop` | `develop` | Xóa nhánh local sau khi merge. |
| `hotfix/*` | Vá lỗi khẩn cấp trực tiếp trên bản Production. | `main` | `main` và đồng bộ lại `develop` | Tạo tag vá `vX.Y.(Z+1)`. |
| `release/*` | Chuẩn bị đóng gói bản phát hành (ví dụ: `release/1.0.1`). | `develop` | `main` và `develop` | Chỉ sửa lỗi bản phát hành, nâng version code/name. |

> **Lưu ý thực tế:** Nếu repo hiện tại chưa có nhánh `develop`, AI/Developer sẽ checkout từ `main` để tạo `develop` hoặc tạo `feature/*` trực tiếp từ nhánh đang hoạt động theo chỉ dẫn của người dùng.

---

## 2. QUY CHUẨN COMMIT MESSAGE (CONVENTIONAL COMMITS)

Mọi commit phải tuân thủ cấu trúc Conventional Commits với mô tả tiếng Việt rõ ràng:

```text
<type>(<scope>): <tiêu đề ngắn gọn bằng tiếng Việt>

<mô tả chi tiết các thay đổi bằng tiếng Việt nếu có>
```

### Các tiền tố (`type`) chuẩn:
- `feat`: Thêm tính năng mới (ví dụ: `feat(ping): bổ sung bộ lọc host ping nhanh`).
- `fix`: Sửa lỗi phần mềm (ví dụ: `fix(traceroute): xử lý crash khi hop không phản hồi`).
- `refactor`: Tái cấu trúc mã nguồn mà không đổi nghiệp vụ (ví dụ: `refactor(ui): tách LiquidGlassCard thành component dùng chung`).
- `perf`: Tối ưu hiệu năng hiển thị, vẽ canvas hoặc bộ nhớ (ví dụ: `perf(chart): cô lập recomposition biểu đồ sóng 120 FPS`).
- `docs`: Cập nhật tài liệu kỹ thuật, plan, process hoặc history.
- `chore`: Cập nhật cấu hình build Gradle, dependencies hoặc kịch bản CI/CD.

---

## 3. QUY TRÌNH PHÁT HÀNH TỰ ĐỘNG (GITHUB ACTIONS RELEASE)

Ứng dụng tích hợp quy trình phát hành tự động qua GitHub Actions (`.github/workflows/release.yml`):

1. Kiểm tra build thành công ở máy cục bộ:
   ```bash
   ./gradlew assembleDebug
   ```
2. Cập nhật `versionCode` và `versionName` trong `app/build.gradle.kts`.
3. Tạo tag phiên bản và push lên GitHub:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. GitHub Action sẽ tự động biên dịch APK phát hành, tạo GitHub Release kèm changelog và đính kèm APK cho người dùng cập nhật tự động trong ứng dụng.