# 📋 Nhật Ký Thực Thi: Tái cấu trúc file JSON từ điển theo nhóm { level, type, passwords: [...] }

- **Ngày thực hiện**: 2026-11-22
- **Agent phụ trách**: `lead-architect`, `network-engine-specialist`, `qa-perf-specialist`
- **Yêu cầu gốc**: Tổ chức file json mật khẩu dạng sau { 'level': 1, Type:'Number', 'Number + String', . 'Password': ['12345678', '11111111'] } ... Dạng vậy thì sẽ dễ dàng khi cần cập nhật hơn. Vì trên app đã có bộ filter này rồi.
- **Trạng thái**: ✅ Hoàn thành

---

## 1. Phân Tích & Kế Hoạch (Analysis & Planning)
- **Bối cảnh & Vấn đề**:
  - Trước đây, file JSON từ điển lưu trữ dưới dạng mảng phẳng từng phần tử riêng lẻ `{ "password": "...", "level": 1 }` (1.684 đối tượng tương ứng 8.430 dòng code), gây cồng kềnh và khó khăn khi cần thêm mới hàng loạt mật khẩu cùng nhóm độ khó hoặc loại ký tự.
  - Người dùng yêu cầu gom nhóm các mật khẩu có cùng cấp độ `level` và cùng loại `type` (Number, Number + String, Complex) vào chung một mảng `passwords: [...]`.
- **Mục tiêu kỹ thuật**:
  1. Tái cấu trúc cả hai file từ điển (`app/src/main/assets/wifi_passwords.json` và `docs/dictionary/wifi_passwords.json`) theo cấu trúc phân nhóm:
     ```json
     {
       "level": 1,
       "type": "Number",
       "passwords": ["00000000", "11111111", "12345678", ...]
     }
     ```
  2. Nâng cấp `WifiScanEngine.kt`: Hàm `parseJsonDictionary` và `parseJsonArrayOfGroups` hỗ trợ linh hoạt:
     - Nhận diện cả `groups` và `items`.
     - Nhận diện cả chữ hoa/thường: `level`/`Level`, `type`/`Type`, `passwords`/`Password`/`password`.
     - Tương thích ngược 100% với định dạng item đơn lẻ `{ "password": "...", "level": ... }` và danh sách chuỗi thô `passwords: [...]`.
  3. Bổ sung trường `type: String = "Number"` vào `DictionaryItem` trong `Models.kt`.
  4. Viết unit test xác thực và đảm bảo biên dịch build APK thành công.

---

## 2. Chi Tiết Thực Hiện (Execution Steps)

### Bước 1: Tái cấu trúc dữ liệu JSON từ điển
- Viết script Python gom 1.684 mẫu mật khẩu thành 51 nhóm tinh gọn theo cặp `(level, type)`:
  - `Number`: 995 mật khẩu chỉ chứa số (0-9).
  - `Number + String`: 450 mật khẩu gồm chữ và số hoặc chữ cái thuần túy.
  - `Complex`: 239 mật khẩu có chứa ký tự đặc biệt (@, #, $, !, ...).
- File JSON giảm từ 8.430 dòng xuống chỉ còn 1.999 dòng, cực kỳ gọn gàng và dễ mở rộng/bảo trì.

### Bước 2: Nâng cấp Data Layer (`Models.kt`)
- `DictionaryItem`: Bổ sung thuộc tính `val type: String = "Number"` với giá trị mặc định để tương thích hoàn toàn mã nguồn cũ.

### Bước 3: Cải tiến Network Engine (`WifiScanEngine.kt`)
- Nâng cấp `parseJsonDictionary(jsonStr: String): List<DictionaryItem>`:
  - Hỗ trợ mảng gốc trực tiếp `[ { level, type, passwords: [...] } ]`.
  - Hỗ trợ đối tượng có trường `groups` hoặc `items`.
  - Trích xuất mảng mật khẩu từ khóa `passwords`, `Password` hoặc `password`.
  - Bổ sung hàm `determinePasswordType(pwd: String)` tự động phát hiện `Number`, `Number + String` hoặc `Complex`.

### Bước 4: Viết bài kiểm thử tự động (`WifiScanEngineTest.kt`)
- `testDeterminePasswordType`: Kiểm tra chính xác phân loại loại mật khẩu.
- `testParseGroupedJsonDictionary`: Kiểm tra parser đọc đúng cấu trúc nhóm `groups: [{ level, type, passwords: [...] }]`.

---

## 3. Danh Sách Tệp Thay Đổi (Impacted Files)
- `[Modified]` `app/src/main/assets/wifi_passwords.json` - Từ điển nhóm mới theo `level`, `type` và `passwords: [...]`.
- `[Modified]` `docs/dictionary/wifi_passwords.json` - Bản lưu từ điển nhóm trên kho mã nguồn.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/model/Models.kt` - Bổ sung `type: String` trong `DictionaryItem`.
- `[Modified]` `app/src/main/java/com/vnpt/pingtool/data/engine/WifiScanEngine.kt` - Parser nhóm linh hoạt, hỗ trợ đa cấu trúc và tự động xác định type.
- `[Modified]` `app/src/test/java/com/vnpt/pingtool/data/engine/WifiScanEngineTest.kt` - Bài test tự động cho parser nhóm và xác định type.
- `[Modified]` `docs/logs/README.md` - Cập nhật mục lục nhật ký.

---

## 4. Kiểm Thử & Xác Minh (Verification & Testing)
- **Unit Tests**:
  - Chạy `./gradlew.bat test` -> `BUILD SUCCESSFUL in 26s` (Toàn bộ bài test đều PASS).
- **Build APK**:
  - Chạy `./gradlew.bat assembleDebug` -> `BUILD SUCCESSFUL in 11s`.

---

## 5. Kết Luận
Cấu trúc từ điển JSON mới dạng nhóm `{ level, type, passwords: [...] }` đã hoạt động hoàn hảo, giảm hơn 75% số dòng JSON, giúp quản lý và cập nhật các tập mật khẩu mới theo bộ lọc trên app cực kỳ thuận tiện và chuẩn xác.
