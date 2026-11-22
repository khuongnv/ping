# 💎 QUY TẮC THIẾT KẾ UI/UX LIQUID GLASS X APPLE MUSIC HIG
## Dự án: VNPT Ping Tool (`com.vnpt.pingtool`)

> **Kế thừa và quy chuẩn hóa từ:** `docs/UI_UX_DESIGN_STANDARDS.md`

---

## 1. BẢNG MÀU TOKEN CHUẨN (DESIGN TOKENS)

Mọi Composable trong ứng dụng phải sử dụng hệ thống màu token chuẩn, tuyệt đối không hardcode mã màu lung tung:

| Token | Dark Mode (Mặc định) | Light Mode | Vị trí áp dụng |
|:---|:---|:---|:---|
| `PrimaryAccent` | `#FA2D55` (Apple Red) | `#FA2D55` | Điểm nhấn chính, nút CTA, Tab active, Sóng Ping trễ thấp |
| `SecondaryAccent` | `#FF375F` | `#E02449` | Trạng thái Pressed, Gradient viền sáng, Hiệu ứng phát sáng |
| `Background` | `#000000` (AMOLED Black) | `#FFFFFF` | Nền toàn màn hình. Dark mode phải là đen tuyền 100% |
| `Surface` | `#1C1C1E` (iOS Dark Gray) | `#F2F2F7` | Nền thẻ kính lỏng (Glass Card), Dialog, Sheet |
| `SurfaceVariant` | `#2C2C2E` | `#E5E5EA` | Nền ô nhập IP, thanh tìm kiếm, container phụ |
| `TextPrimary` | `#FFFFFF` (100% Alpha) | `#000000` | Tiêu đề chính, giá trị RTT, IP Address |
| `TextSecondary` | `#8E8E93` (Gray93) | `#8E8E93` | Nhãn phụ, tiêu đề cột, chú thích đơn vị đo ms |
| `HairlineDivider` | `rgba(255,255,255,0.08)` | `rgba(0,0,0,0.08)` | Đường phân cách siêu mỏng 0.5dp |
| `LatencyGood` | `#30D158` (iOS Green) | `#34C759` | Độ trễ mạng tốt (< 50ms) |
| `LatencyMedium` | `#FFD60A` (iOS Yellow) | `#FFCC00` | Độ trễ trung bình (50ms - 150ms) |
| `LatencyPoor` | `#FF453A` (iOS Red) | `#FF3B30` | Độ trễ cao (> 150ms) hoặc mất gói (Packet Loss) |

---

## 2. CHUẨN THẺ KÍNH LỎNG (LIQUID GLASS CARDS)

```kotlin
// Mẫu Liquid Glass Card chuẩn
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val glassBg = if (isDark) Color(0xEE1E1E22) else Color(0xF2FFFFFF)
    val rimColor = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Surface(
        modifier = modifier
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .border(width = 0.75.dp, color = rimColor, shape = RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = glassBg
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
```

---

## 3. QUY TẮC HIỂN THỊ VÀ TỐI ƯU HIỆU NĂNG

1. **Hiển thị văn bản (Text Truncation):**
   - Mọi tiêu đề hoặc địa chỉ host/IP trong danh sách phải có `maxLines = 1` và `overflow = TextOverflow.Ellipsis`.
2. **Thanh điều hướng đáy nổi (Floating Bottom Bar):**
   - Bo góc `24dp`–`26dp`, cách đáy `12dp` và 2 bên mép `16dp`.
   - Tab được chọn bọc trong Capsule Pill nền `PrimaryAccent.copy(alpha = 0.12f)` và viền `0.75dp`.
3. **Biểu đồ sóng Realtime Canvas:**
   - Vẽ trực tiếp trên `Canvas` với `drawPath()`, không dùng các thư viện chart nặng nề gây tụt khung hình.
   - Luôn duy trì tốc độ tối thiểu **60 FPS** (ưu tiên **120 FPS ProMotion**).
4. **Đường phân cách Inset (Inset Dividers):**
   - Độ dày `0.5dp`, thụt vào thẳng hàng với lề chữ (`72dp`), tuyệt đối không cắt ngang qua icon/thumbnail.
5. **Hiệu ứng vật lý (Spring Physics):**
   - Không dùng animation tuyến tính cứng. Dùng lò xo `Spring.DampingRatioMediumBouncy` và `Spring.StiffnessLow`.

---

## 4. NHỮNG ĐIỀU CẤM KỴ

- ❌ **CẤM:** Dùng nền màu xám nhờ nhờ ở Dark mode thay vì `#000000`.
- ❌ **CẤM:** Nút bấm và card có góc nhọn hoắt (Corner Radius < 8dp).
- ❌ **CẤM:** Recomposition tràn lan do không đặt `key` trong `LazyColumn`.
- ❌ **CẤM:** Quảng cáo hoặc popup gây rối màn hình (Triết lý Zero Ads tuyệt đối).