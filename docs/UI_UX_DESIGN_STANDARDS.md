# 💎 TIÊU CHUẨN THIẾT KẾ UI/UX & HỆ THỐNG GIAO DIỆN CHUẨN (UI/UX DESIGN STANDARDS & STYLE GUIDE)

> **Tài liệu tham chiếu chuẩn hóa dành cho các dự án phần mềm học tập và kế thừa phong cách thiết kế của dự án Android Music Player (Phong cách Apple Music x Liquid Glass).**

---

## 📌 1. Triết Lý Thiết Kế Cốt Lõi (Core Design Philosophy)

Mọi sản phẩm áp dụng tiêu chuẩn này phải xây dựng xoay quanh 4 nguyên lý nền tảng:

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        4 TRỤ CỘT THIẾT KẾ                              │
│                                                                        │
│  1. CONTENT-FIRST        Tôn vinh nội dung trọng tâm. Giao diện là sân │
│                          khấu, không được lấn át tác phẩm/dữ liệu.     │
│                                                                        │
│  2. LIQUID GLASS         Vật liệu kính lỏng bán trong suốt, đổ bóng đa │
│                          lớp và viền sáng phát quang (luminous rim).   │
│                                                                        │
│  3. SPRING PHYSICS       Chuyển động có độ đàn hồi tự nhiên như cơ cấu │
│                          vật lý thực, tuyệt đối tránh chuyển động đơ.  │
│                                                                        │
│  4. BOLD HIERARCHY       Phân cấp thông tin rõ ràng với tiêu đề lớn,   │
│                          khoảng thở thoáng đạt và độ tương phản cao.   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 2. Hệ Thống Màu Sắc & Bảng Token (Design Tokens & Color System)

### 2.1. Bảng Token Màu Chuẩn (Color Tokens)

Hệ thống màu sắc phải đảm bảo tính đối lập sắc nét giữa Dark Theme và Light Theme, không dùng màu xám nhờ nhờ ở nền đen:

| Token | Dark Mode | Light Mode | Ý Nghĩa & Vị Trí Áp Dụng |
| :--- | :--- | :--- | :--- |
| **`PrimaryAccent`** | `#FA2D55` (Apple Red) | `#FA2D55` (Apple Red) | Điểm nhấn thương hiệu, Active Tab, CTA chính, Slider Track, Nút Play |
| **`SecondaryAccent`**| `#FF375F` | `#E02449` | Trạng thái Hover, Pressed, Gradient viền phát sáng |
| **`Background`** | `#000000` (AMOLED Black) | `#FFFFFF` (Pure White) | Nền gốc toàn màn hình. Dark mode phải là đen tuyền 100% |
| **`Surface`** | `#1C1C1E` (iOS Dark Gray) | `#F2F2F7` (iOS Light Gray) | Nền thẻ (Card), Hộp thoại (Dialog), Container phụ |
| **`SurfaceVariant`**| `#2C2C2E` | `#E5E5EA` | Nền elevated, thanh tìm kiếm, trạng thái chọn phụ |
| **`TextPrimary`** | `#FFFFFF` (100% Alpha) | `#000000` (100% Alpha) | Tiêu đề chính, tên bài hát, nhãn quan trọng |
| **`TextSecondary`** | `#8E8E93` (Gray93) | `#8E8E93` (Gray93) | Phụ đề, tên nghệ sĩ, thời lượng, nhãn trạng thái |
| **`HairlineDivider`**| `rgba(255,255,255,0.08)` | `rgba(0,0,0,0.08)` | Đường phân cách siêu mỏng 0.5dp, đường viền danh sách |

---

## 🫧 3. Phong Cách "Liquid Glass" (Vật Liệu Kính Lỏng & Chiều Sâu)

Phong cách kính lỏng tạo cảm giác các khối giao diện nổi lơ lửng trên nền nội dung với chất liệu kính khói phản chiếu ánh sáng:

```text
┌────────────────────────────────────────────────────────────────────────┐
│  MÔ PHỎNG MẶT CẮT VẬT LIỆU LIQUID GLASS                                │
│                                                                        │
│  ┌────────────────────────────────────────────────────────────┐        │
│  │ ✨ Viền phản quang siêu mảnh (0.75dp Luminous Rim)          │        │
│  │ ────────────────────────────────────────────────────────── │        │
│  │ 🌫️ Kính bán trong suốt (Alpha 88% - 92% + Background Blur)  │        │
│  │                                                            │        │
│  │ 💊 Capsule Pill bọc bên trong với hiệu ứng phát sáng nhẹ   │        │
│  └────────────────────────────────────────────────────────────┘        │
│      ░░░░░░░░░░░░░░░ Đổ bóng mềm đa tầng (Elevation 8dp - 12dp)        │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.1. Thông số kỹ thuật của Liquid Glass Card:
- **Độ bo góc (Corner Radius)**:
  - Thẻ thông thường: `14.dp` – `18.dp`.
  - Floating Dock / Bottom Bar: `24.dp` – `26.dp`.
  - Nút bấm / Quick Action Pill: `20.dp` – `50.dp` (Full capsule).
- **Màu nền kính (Glass Fill)**:
  - **Dark Mode**: `Color(0xEE1E1E22)` (Độ mờ ~93% trên nền tối).
  - **Light Mode**: `Color(0xF2FFFFFF)` (Độ mờ ~95% trên nền sáng).
- **Viền sáng phát quang (Luminous Rim Border)**:
  - **Dark Mode**: Viền `0.75.dp` với màu `Color(0x33FFFFFF)` (20% trắng bạc) tạo cảm giác cạnh kính vát khúc xạ ánh sáng.
  - **Light Mode**: Viền `0.75.dp` với màu `Color(0x1F000000)` (12% xám khói) chống lóa viền.
- **Đổ bóng (Drop Shadow)**:
  - Đổ bóng mềm: `shadow(elevation = 10.dp, shape = RoundedCornerShape(24.dp), clip = false)`.

---

## 🔤 4. Quy Chuẩn Kiểu Chữ (Typography Hierarchy)

Áp dụng chuẩn **Apple Human Interface Guidelines (HIG)**:

| Cấp Bậc | Kích Thước (sp) | Độ Đậm (Weight) | Letter Spacing | Vị Trí & Mục Đích |
| :--- | :--- | :--- | :--- | :--- |
| **Large Title** | `34.sp` | **Bold** (700) | `0.37.sp` | Tiêu đề chính đầu trang (Thư viện, Ký ức, Cài đặt) |
| **Headline** | `22.sp` | **SemiBold** (600) | `0.35.sp` | Tiêu đề phân mục lớn, tên Album chi tiết |
| **Title Medium** | `17.sp` | **SemiBold** (600) | `-0.41.sp` | Tên bài hát, tên mục trong danh sách |
| **Body Large** | `16.sp` | **Medium** (500) | `-0.20.sp` | Tiêu đề cài đặt, văn bản đọc chính |
| **Body Medium** | `14.sp` – `15.sp` | **Normal** (400) | `-0.15.sp` | Tên ca sĩ, tên album, mô tả phụ (`Gray93`) |
| **Caption / Label** | `12.sp` – `13.sp` | **Medium** (500) | `0.80.sp` | Nhãn phân mục in hoa, mốc thời gian, badge phiên bản |

> **Quy tắc cắt chữ (Text Truncation)**: Mọi dòng tiêu đề bài hát, danh mục hoặc phụ đề trong danh sách phải đặt `maxLines = 1` kèm `overflow = TextOverflow.Ellipsis`. Không bao giờ để chữ tràn làm vỡ layout.

---

## 📱 5. Giải Phẫu Các Thành Phần Tiêu Biểu (Component Anatomy)

### 5.1. Thanh Điều Hướng Đáy Nổi (Floating Liquid Glass Bottom Bar)
```text
┌─────────────────────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────────┐ │
│ │   ┌────────┐                                            │ │
│ │   │(★) Thư │   (🎵) Bài hát    (✨) Ký ức    (⚙) Cài đặt │ │
│ │   └────────┘                                            │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```
- **Kiểu dáng**: Dock nổi tách rời mép đáy (`padding(horizontal = 16.dp, bottom = 12.dp)`).
- **Active Indicator**: Mục đang chọn được bao quanh bởi viên thuốc kính lỏng (Capsule Pill):
  - Bo cong: `RoundedCornerShape(20.dp)`.
  - Nền viên thuốc: `PrimaryAccent.copy(alpha = 0.12f)`.
  - Viền viên thuốc: `PrimaryAccent.copy(alpha = 0.28f)`.
  - Màu icon và chữ active: `PrimaryAccent` (`#FA2D55`).

---

### 5.2. Thanh Phát Nhạc Nổi (Floating MiniPlayer)
```text
┌─────────────────────────────────────────────────────────────┐
│ ┌───┐  Tên Bài Hát Đang Phát                       (▶) (⏭)  │
│ │ 🖼│  Tên Ca Sĩ • Album                                    │
│ └───┘                                                       │
│ ═══════════════════════════════════════════════════════════ │ <- Hairline Progress
└─────────────────────────────────────────────────────────────┘
```
- **Kích thước**: Chiều cao `60.dp` – `64.dp`, bo góc `20.dp`, lề ngang `16.dp`.
- **Thanh tiến trình Hairline**: Dày `2.dp` gắn sát mép đáy thẻ với màu `PrimaryAccent`, thể hiện vị trí phát nhạc hiện tại mà không làm rối mắt.
- **Tương tác cử chỉ**:
  - `Tap`: Mở toàn màn hình (Now Playing).
  - `Swipe Left / Right`: Chuyển bài trước/sau với phản hồi rung haptic.

---

### 5.3. Màn Hình Phát Toàn Phần (Hero NowPlayingScreen)
```text
┌─────────────────────────────────────────────────────────────┐
│              (⌄)           Đang Phát          (⋯)           │
│                                                             │
│               ┌─────────────────────────────┐               │
│               │                             │               │
│               │        HERO ARTWORK         │               │
│               │        (Tỉ lệ 1:1)          │               │
│               │  Scale 1.0 (Playing)        │               │
│               │  Scale 0.85 (Paused)        │               │
│               │                             │               │
│               └─────────────────────────────┘               │
│                                                             │
│   Tiêu Đề Bài Hát Tuyệt Phẩm                       (♥)      │
│   Tên Nghệ Sĩ • Album Thượng Hạng                           │
│                                                             │
│   ──────────●────────────────────────────────────────────   │
│   02:15                                            -03:40   │
│                                                             │
│        (🔀)      (⏮)        ( ▶ )        (⏭)      (🔁)      │
│                                                             │
│        (🔈) ──────────────────●──────────────── (🔊)        │
└─────────────────────────────────────────────────────────────┘
```
- **Nền Ambient Blur**: Trích xuất màu chủ đạo (Dominant Color) từ ảnh bìa và phủ lớp Gaussian Blur diện rộng (`blur(60.dp)`), kết hợp lớp phủ chuyển tiếp tối dần (Vignette) để bảo toàn độ tương phản cho nút bấm.
- **Hiệu ứng vật lý ảnh bìa (Artwork Spring Physics)**:
  - Khi đang phát (Playing): `Scale = 1.0f`.
  - Khi tạm dừng (Paused): Co nhẹ lại `Scale = 0.85f` với độ nảy nhẹ.
- **Thanh tua bài (Scrubbing Bar)**:
  - Track mỏng thanh thoát `4.dp` – `5.dp`.
  - Nút kéo (Thumb): Ẩn hoặc siêu nhỏ khi bình thường, phóng lớn lên `14.dp` – `16.dp` khi người dùng chạm vuốt (Scrubbing).

---

### 5.4. Mục Danh Sách Chuẩn (List Items with Inset Dividers)
```text
┌────┬────────────────────────────────────────────────────┬───┐
│ 🖼 │ Tiêu đề bài hát (16sp SemiBold)                   │(⋯)│
│48dp│ Nghệ sĩ • 04:12 (14sp Gray93)                      │   │
└────┴────────────────────────────────────────────────────┴───┘
       ───────────────────────────────────────────────────  <- Inset Hairline Divider
```
- **Đường kẻ phân cách Inset (Bắt buộc)**:
  - Độ dày `0.5.dp`.
  - Bắt đầu thẳng hàng với lề chữ (thường thụt vào `72.dp`), **tuyệt đối không cắt ngang qua ảnh thumbnail**.

---

## 🏃 6. Chuyển Động & Phản Hồi Xúc Giác (Motion & Haptics)

### 6.1. Nguyên Tắc Lò Xo (Apple Spring Animation)
Tuyệt đối không sử dụng chuyển động tuyến tính (Linear) hoặc gia tốc cứng (Hard Ease In/Out). Mọi biến đổi trạng thái (ẩn/hiện, mở rộng, co giãn) phải dùng thông số lò xo:

```kotlin
// Khuyến nghị thông số lò xo chuẩn Apple
val springSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy, // Độ nảy vừa phải (0.7f - 0.75f)
    stiffness = Spring.StiffnessLow                 // Độ cứng mềm mại tạo chuyển động êm
)
```

### 6.2. Phản Hồi Xúc Giác Cơ Học (Haptic Feedback)
Tạo cảm giác chân thực như đang bấm công tắc vật lý:
- Nhấn các nút chính (**Play/Pause**, **Skip**, **Favorite**): Kích hoạt `HapticFeedbackType.LongPress` hoặc `TextHandleMove`.
- Vuốt thanh tiến trình (Scrubbing): Rung siêu nhẹ mỗi khi vượt qua mốc thời gian lớn.

---

## ⚡ 7. Tối Ưu Hiệu Năng Hiển Thị (Performance Standards)

Giao diện đẹp chỉ thực sự giá trị khi đạt chuẩn mượt mà tối thiểu **60 FPS** (ưu tiên **120 FPS ProMotion**):

1. **Cô lập Recomposition (Recomposition Isolation)**:
   - Mọi trạng thái UI (`UiState`) phải là `data class` bất biến (`@Immutable`).
   - Sử dụng `derivedStateOf` cho các phép tính toán phụ thuộc trạng thái cuộn hoặc tiến trình phát nhạc.
2. **Khóa định danh trong danh sách (`key` trong Lazy Lists)**:
   - Mọi `LazyColumn`, `LazyRow`, `LazyVerticalGrid` bắt buộc phải truyền `key`:
     ```kotlin
     items(items = songs, key = { it.id }) { song -> ... }
     ```
3. **Quản lý bộ nhớ hình ảnh (Coil / Image Loading)**:
   - Luôn sử dụng bộ nén ảnh kích thước vừa vặn khung hiển thị (`size` mapping).
   - Tích hợp hiệu ứng chuyển cảnh mềm (`crossfade(300)`).
   - Ảnh bìa album tối ưu hóa lưu trữ ở độ phân giải `800x800` px (tỉ lệ 1:1), nén chất lượng cao (~150 - 200 KB) để tránh tràn bộ nhớ Bitmap (OOM).

---

## 🚫 8. Những Điều Cấm Kỵ Trong Thiết Kế (Anti-Patterns)

```text
❌ CẤM: Dùng nền màu xám nhờ nhờ ở Dark mode thay cho màu đen AMOLED thuần khiết (#000000).
❌ CẤM: Đường kẻ phân cách (Divider) kéo dài cắt ngang qua ảnh đại diện/thumbnail.
❌ CẤM: Dùng thanh Slider mặc định to thô kệch, không có hiệu ứng phóng to thumb khi vuốt.
❌ CẤM: Nút bấm và card có góc nhọn hoắt (Corner Radius < 8dp).
❌ CẤM: Trộn lẫn các màu Accent khác nhau (như xanh lá, vàng, xanh dương) làm loãng màu chủ đạo Apple Red.
❌ CẤM: Chữ tiêu đề dài bị nhảy dòng làm lệch hàng (bắt buộc dùng maxLines = 1 + Ellipsis).
❌ CẤM: Bỏ qua trạng thái Loading dạng Skeleton Shimmer hoặc hiệu ứng chuyển trang bị giật khựng.
```

---

## ✅ 9. Bảng Kiểm Tra Nghiệm Thu UI/UX (Definition of Done)

Trước khi nghiệm thu bất kỳ màn hình hoặc tính năng mới nào, hãy đối chiếu danh sách kiểm tra sau:

- [ ] **Màu sắc**: Đã sử dụng mã màu chuẩn từ bảng token; Dark Mode hiển thị trên nền đen `#000000`.
- [ ] **Chất liệu Liquid Glass**: Các dock nổi, miniplayer, dialog có bo góc lớn (`20dp` – `26dp`), viền sáng khúc xạ (`0.75dp`) và đổ bóng mềm.
- [ ] **Phân cấp kiểu chữ**: Tiêu đề màn hình chuẩn Large Title (34sp Bold); phụ đề màu `Gray93`.
- [ ] **Đường kẻ Inset**: Đường kẻ chia cách mục danh sách thụt vào khớp với lề văn bản (`72dp`).
- [ ] **Tương tác**: Nút bấm có hiệu ứng nhấn co giãn nhẹ hoặc phản hồi rung Haptic.
- [ ] **Chuyển động đàn hồi**: Áp dụng Spring Physics cho các hiệu ứng phóng to/thu nhỏ và mở rộng.
- [ ] **Đồng bộ Dark & Light Theme**: Chuyển đổi giữa 2 chế độ sáng/tối không bị lệch màu, mất chữ hay lỗi tương phản.
- [ ] **Hiệu năng**: Danh sách cuộn đạt 60/120 FPS, không có hiện tượng giật khung hình (frame drops).
