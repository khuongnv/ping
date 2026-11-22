# GIT BRANCH STRATEGY
## Quy chuẩn quản lý Git Branch cho dự án Fullstack

- **Tên tài liệu:** Git Branch Strategy
- **Phạm vi:** Frontend, Backend và Database
- **Nền tảng khuyến nghị:** GitLab
- **Phiên bản tài liệu:** 1.0
- **Trạng thái:** Đề xuất áp dụng

---

## 1. Mục đích

Tài liệu quy định cách tổ chức và sử dụng Git Branch trong dự án Fullstack nhằm:

- Thống nhất quy trình phát triển giữa các thành viên.
- Tách biệt code đang phát triển, code kiểm thử và code Production.
- Kiểm soát thay đổi thông qua Merge Request (MR) và Code Review.
- Hạn chế lỗi phát sinh khi tích hợp Frontend, Backend và Database.
- Hỗ trợ tự động hóa Build, Test và Deploy bằng GitLab CI/CD.
- Có thể truy vết thay đổi theo chức năng, lỗi và phiên bản phát hành.

## 2. Phạm vi áp dụng

Quy chuẩn áp dụng cho toàn bộ mã nguồn và tài nguyên kỹ thuật của dự án, bao gồm:

| Thành phần | Ví dụ |
|---|---|
| Frontend | Angular, React, Vue |
| Backend | ASP.NET Core, Java, Node.js |
| Database | Oracle SQL, PL/SQL, migration scripts |
| DevOps | Dockerfile, GitLab CI/CD, Kubernetes manifests |
| Tài liệu | API documentation, thiết kế kỹ thuật, hướng dẫn triển khai |

**Nguyên tắc:** Branch được tổ chức theo mục đích phát triển và vòng đời phát hành, không chia branch riêng cho Frontend và Backend. Một chức năng Fullstack có thể thay đổi nhiều thành phần trong cùng một feature branch.

## 3. Mô hình Branch

### 3.1. Các branch chính

| Branch | Mục đích | Nguồn tạo | Nơi merge |
|---|---|---|---|
| `main` | Mã nguồn đã phát hành Production | Khởi tạo dự án | Nhận từ `release/*`, `hotfix/*` |
| `develop` | Tích hợp các chức năng đang phát triển | `main` hoặc khởi tạo ban đầu | Nhận từ `feature/*`, `bugfix/*`, `refactor/*` |
| `feature/*` | Phát triển chức năng mới | `develop` | `develop` |
| `bugfix/*` | Sửa lỗi thông thường | `develop` | `develop` |
| `refactor/*` | Tái cấu trúc, cải thiện code | `develop` | `develop` |
| `release/*` | Ổn định và chuẩn bị phát hành | `develop` | `main` và đồng bộ lại `develop` |
| `hotfix/*` | Sửa lỗi khẩn cấp trên Production | `main` | `main` và đồng bộ lại `develop` |

### 3.2. Sơ đồ tổng quan

```text
                         feature/login
                        /
                       /             release/1.0.0
                      /             /
main ----------------+-------------+-------------------->
       \              \           /                    /
        \              develop --+                    /
         \             /   \                         /
          \ hotfix/1.0.1   feature/employee          /
           \             \                          /
            +-------------+-------------------------+
```

Luồng chuẩn:

```text
feature/*  ─┐
bugfix/*   ─┼──> develop ───> release/* ───> main
refactor/* ┘                    │
                                └──> đồng bộ lại develop

main ───> hotfix/* ───> main
                   └──> đồng bộ lại develop
```

> Sơ đồ mang tính khái quát. Mọi thao tác merge phải thực hiện thông qua Merge Request và tuân thủ quy định bảo vệ branch.

### 3.3. Ý nghĩa từng branch

#### `main`

- Chứa mã nguồn tương ứng với phiên bản đã được phê duyệt phát hành.
- Không cho phép Developer push trực tiếp.
- Không merge chức năng chưa hoàn thành hoặc chưa kiểm thử.
- Mỗi lần phát hành chính thức phải tạo Git Tag.
- Chỉ nhận thay đổi từ `release/*` hoặc `hotfix/*`.

#### `develop`

- Là branch tích hợp các chức năng mới.
- Có thể chứa nhiều chức năng đã hoàn thành phát triển nhưng chưa phát hành Production.
- Là nguồn tạo các branch `feature/*`, `bugfix/*`, `refactor/*` và `release/*`.
- Không cho phép push trực tiếp nếu dự án áp dụng kiểm soát bằng MR.

#### `feature/*`

- Dùng để phát triển chức năng mới.
- Được tạo từ `develop`.
- Có thể chứa thay đổi đồng thời ở Frontend, Backend và Database nếu chúng thuộc cùng một chức năng.
- Sau khi hoàn thành, tạo MR vào `develop`.
- Xóa branch sau khi merge thành công, trừ trường hợp cần giữ lại theo chính sách dự án.

#### `bugfix/*`

- Dùng để sửa lỗi phát hiện trong quá trình phát triển hoặc kiểm thử.
- Được tạo từ `develop`.
- Sau khi sửa và kiểm thử, tạo MR vào `develop`.

#### `refactor/*`

- Dùng để tái cấu trúc code mà không làm thay đổi hành vi nghiệp vụ ngoài phạm vi được duyệt.
- Được tạo từ `develop`.
- Phải có kiểm thử phù hợp để đảm bảo không gây ảnh hưởng chức năng hiện hữu.

#### `release/*`

- Dùng để ổn định phiên bản trước khi phát hành.
- Được tạo từ `develop`.
- Chỉ cho phép sửa lỗi, cập nhật version, cấu hình phát hành và tài liệu liên quan.
- Không bổ sung chức năng mới trong giai đoạn release.
- Sau khi UAT và kiểm thử hồi quy đạt yêu cầu, merge vào `main`, tạo tag và đồng bộ các sửa lỗi cần thiết về `develop`.

#### `hotfix/*`

- Dùng để xử lý lỗi nghiêm trọng trên Production.
- Được tạo trực tiếp từ `main`.
- Chỉ chứa thay đổi cần thiết để khắc phục sự cố.
- Sau khi kiểm thử và phê duyệt, merge vào `main`, tạo tag phiên bản vá và đồng bộ thay đổi về `develop`.
- Nếu đang tồn tại `release/*`, cần đánh giá và đồng bộ hotfix vào release branch nếu bản release tiếp theo có khả năng bị ảnh hưởng.

## 4. Quy tắc đặt tên Branch

### 4.1. Cấu trúc

```text
<prefix>/<ticket-id>-<short-description>
```

Nếu dự án không sử dụng hệ thống quản lý ticket, có thể bỏ `<ticket-id>`.

### 4.2. Prefix quy định

| Prefix | Mục đích | Ví dụ |
|---|---|---|
| `feature/` | Chức năng mới | `feature/HNI-101-login-sso` |
| `bugfix/` | Sửa lỗi thông thường | `bugfix/HNI-102-fix-api-timeout` |
| `hotfix/` | Sửa lỗi Production | `hotfix/HNI-103-fix-login-prod` |
| `refactor/` | Tái cấu trúc code | `refactor/HNI-104-employee-service` |
| `release/` | Chuẩn bị phát hành | `release/1.2.0` |
| `chore/` | Công việc kỹ thuật, bảo trì | `chore/HNI-105-update-angular` |
| `docs/` | Cập nhật tài liệu | `docs/HNI-106-api-documentation` |

### 4.3. Quy tắc đặt tên

- Sử dụng chữ thường.
- Dùng dấu gạch ngang `-` để phân tách từ.
- Không sử dụng dấu tiếng Việt hoặc khoảng trắng.
- Tên branch ngắn gọn, mô tả đúng mục đích.
- Ưu tiên mã ticket để truy vết công việc.

## 5. Quy trình phát triển chức năng

### 5.1. Tạo feature branch

Cập nhật `develop` trước khi tạo branch:

```bash
git checkout develop
git pull origin develop

git checkout -b feature/HNI-101-login-sso
git push -u origin feature/HNI-101-login-sso
```

### 5.2. Phát triển và commit

Thành viên thực hiện công việc trên branch của mình. Một chức năng Fullstack có thể bao gồm:

```text
feature/HNI-101-login-sso
├── Frontend
│   └── Màn hình đăng nhập, xử lý token
├── Backend
│   └── API xác thực, cấp token
├── Database
│   └── Script hoặc migration nếu cần
└── Tests
    └── Unit Test, Integration Test
```

Commit thường xuyên với nội dung rõ ràng. Không commit file chứa thông tin bí mật.

### 5.3. Đồng bộ code mới từ develop

Trong quá trình phát triển, cần cập nhật thay đổi mới từ `develop` để giảm xung đột:

```bash
git fetch origin
git merge origin/develop
```

Nếu team thống nhất sử dụng rebase cho branch cá nhân, có thể thay bằng:

```bash
git fetch origin
git rebase origin/develop
```

Không rebase branch dùng chung hoặc branch đã được nhiều người khác làm việc nếu chưa thống nhất với team.

### 5.4. Tạo Merge Request

Khi hoàn thành:

1. Push code lên remote.
2. Tạo Merge Request từ feature branch vào `develop`.
3. Mô tả chức năng, phạm vi thay đổi và cách kiểm thử.
4. Gắn ticket/issue liên quan.
5. Đảm bảo pipeline thành công.
6. Hoàn thành Code Review và xử lý các góp ý.
7. Người có quyền thực hiện merge.

### 5.5. Sau khi merge

- Xác nhận chức năng đã được tích hợp vào `develop`.
- Xóa branch feature sau khi merge nếu không cần giữ.
- Cập nhật trạng thái ticket.
- Không tiếp tục phát triển trên branch đã merge.

## 6. Quy trình phát hành phiên bản

### 6.1. Tạo release branch

Khi các chức năng của phiên bản đã hoàn thành và được chấp nhận tích hợp:

```bash
git checkout develop
git pull origin develop

git checkout -b release/1.0.0
git push -u origin release/1.0.0
```

### 6.2. Kiểm thử release

Thực hiện các hoạt động:

- Build Frontend và Backend.
- Unit Test và Integration Test.
- Kiểm thử API.
- Kiểm thử nghiệp vụ và hồi quy.
- Kiểm thử tương thích Database.
- UAT và xác nhận của bộ phận nghiệp vụ.
- Kiểm tra cấu hình triển khai và rollback.

Lỗi phát hiện trong release được sửa trực tiếp trên `release/*`, sau đó phải đồng bộ về `develop`.

### 6.3. Phát hành Production

Sau khi release được phê duyệt:

```bash
git checkout main
git pull origin main
```

Tạo Merge Request từ `release/1.0.0` vào `main`. Sau khi merge, tạo tag:

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

Đồng bộ các sửa lỗi phát sinh trong release về `develop` bằng Merge Request hoặc quy trình merge được team thống nhất.

**Lưu ý:** Không nên chỉ merge release vào `main` rồi bỏ qua việc đồng bộ các commit sửa lỗi trên release về `develop`.

## 7. Quy trình xử lý Hotfix

Khi phát hiện lỗi nghiêm trọng trên Production:

```bash
git checkout main
git pull origin main

git checkout -b hotfix/1.0.1
```

Thực hiện:

1. Xác định nguyên nhân lỗi.
2. Thực hiện thay đổi tối thiểu cần thiết.
3. Build và kiểm thử.
4. Tạo MR từ `hotfix/1.0.1` vào `main`.
5. Sau khi được phê duyệt, merge và tạo tag `v1.0.1`.
6. Tạo MR đồng bộ hotfix vào `develop`.
7. Kiểm tra các release branch đang tồn tại để quyết định có cần đồng bộ hay không.

Không sử dụng hotfix để đưa chức năng mới lên Production.

## 8. Quy chuẩn Commit Message

Khuyến nghị sử dụng Conventional Commits:

```text
<type>(<scope>): <description>
```

Ví dụ:

```text
feat(auth): add Microsoft SSO login
feat(employee): add employee management API
fix(auth): handle expired refresh token
fix(report): correct monthly report calculation
refactor(employee): optimize employee service
docs(api): update authentication documentation
test(auth): add login integration tests
chore(deps): update Angular dependencies
```

Các type phổ biến:

| Type | Ý nghĩa |
|---|---|
| `feat` | Thêm chức năng |
| `fix` | Sửa lỗi |
| `refactor` | Tái cấu trúc |
| `docs` | Tài liệu |
| `test` | Kiểm thử |
| `chore` | Công việc bảo trì |
| `build` | Thay đổi build |
| `ci` | Thay đổi CI/CD |
| `perf` | Cải thiện hiệu năng |
| `revert` | Hoàn tác commit |

## 9. Quy định Merge Request

### 9.1. Thông tin bắt buộc

Mỗi MR cần có:

- Tiêu đề mô tả rõ nội dung thay đổi.
- Ticket/Issue liên quan nếu có.
- Mục tiêu và phạm vi thay đổi.
- Danh sách các thành phần bị ảnh hưởng.
- Hướng dẫn kiểm thử.
- Ảnh chụp màn hình hoặc kết quả API nếu cần.
- Thông tin Database migration nếu có.
- Rủi ro và phương án rollback nếu thay đổi có ảnh hưởng lớn.

### 9.2. Quy định review

- Không tự phê duyệt MR của chính mình.
- Ít nhất một người có trách nhiệm review và phê duyệt.
- MR thay đổi kiến trúc, bảo mật hoặc Database quan trọng cần người phụ trách chuyên môn review.
- Phải xử lý các góp ý bắt buộc trước khi merge.
- Không bỏ qua pipeline thất bại nếu chưa có xác nhận và lý do hợp lệ.

### 9.3. Merge strategy

Khuyến nghị:

- Dùng Squash and Merge cho feature branch để lịch sử `develop` gọn.
- Dùng merge commit hoặc phương thức được team thống nhất cho release/hotfix để bảo toàn lịch sử phát hành.
- Không force push lên `main`, `develop`, `release/*`.

## 10. Quy định bảo vệ Branch trên GitLab

| Thiết lập | `main` | `develop` | `release/*` |
|---|---|---|---|
| Protected Branch | Bắt buộc | Bắt buộc | Bắt buộc |
| Push trực tiếp | Không | Không | Không |
| Merge qua MR | Bắt buộc | Bắt buộc | Bắt buộc |
| Pipeline thành công | Bắt buộc | Bắt buộc | Bắt buộc |
| Code Review | Bắt buộc | Bắt buộc | Bắt buộc |
| Force push | Không | Không | Không |

Phân quyền tham khảo:

| Vai trò | Quyền đề xuất |
|---|---|
| Developer | Tạo branch, push branch cá nhân, tạo MR |
| Reviewer | Review và phê duyệt MR |
| Team Leader / Maintainer | Quản lý MR, merge theo quy định |
| DevOps | Quản lý pipeline, runner, biến môi trường và triển khai |
| Release Manager | Phê duyệt và điều phối phát hành |

Quyền thực tế cần căn cứ vào vai trò và chính sách bảo mật của tổ chức.

## 11. Quy định đối với Frontend, Backend và Database

### 11.1. Frontend

- Kiểm tra format, lint và build trước khi merge.
- Không commit thư mục build hoặc dependency không cần thiết.
- Không đưa API key, token hoặc secret vào mã nguồn.
- Cấu hình môi trường phải tách biệt khỏi source code.

### 11.2. Backend

- Build và chạy Unit Test trước khi merge.
- Kiểm tra API contract và khả năng tương thích ngược.
- Không commit connection string hoặc mật khẩu.
- Các thay đổi ảnh hưởng API phải được thông báo cho nhóm Frontend và các hệ thống liên quan.

### 11.3. Database

- Script SQL, PL/SQL và migration phải được quản lý bằng Git.
- Thay đổi cấu trúc bảng, index, procedure, package và dữ liệu danh mục phải có script tương ứng.
- Không thực hiện thay đổi Production trực tiếp mà không có quy trình phê duyệt và ghi nhận.
- Cần đánh giá tính tương thích giữa phiên bản Backend và Database.
- Phải có phương án rollback hoặc phương án khôi phục phù hợp.
- Không đưa dữ liệu thật, dữ liệu cá nhân hoặc thông tin nhạy cảm vào repository.

Đối với Oracle, nên tổ chức script theo phiên bản hoặc theo chức năng, ví dụ:

```text
database/
├── migrations/
│   ├── V1.0.0/
│   │   ├── 001_create_employee_table.sql
│   │   ├── 002_create_employee_package.sql
│   │   └── 003_create_employee_indexes.sql
│   └── V1.0.1/
│       └── 001_add_employee_status.sql
├── rollback/
│   ├── V1.0.0/
│   └── V1.0.1/
└── README.md
```

Tên file và thứ tự chạy phải thống nhất, có kiểm tra trạng thái trước khi thực thi để tránh chạy lặp ngoài ý muốn.

## 12. Quy định CI/CD

Pipeline tối thiểu nên có các giai đoạn:

```text
Validate
   |
   v
Build
   |
   v
Unit Test
   |
   v
Code Quality / Security Scan
   |
   v
Package Artifact
   |
   v
Deploy Dev
   |
   v
Integration Test
   |
   v
Deploy UAT (phê duyệt)
   |
   v
Deploy Production (phê duyệt)
```

Quy định:

- MR vào `develop`: chạy Validate, Build, Test và kiểm tra chất lượng.
- MR vào `release/*`: chạy đầy đủ kiểm thử cần thiết.
- Merge vào `main` hoặc tạo tag phát hành: tạo artifact có version xác định.
- Deploy Production phải có bước phê duyệt theo chính sách tổ chức.
- Ưu tiên build artifact một lần và promote cùng artifact qua các môi trường.
- Secret phải được quản lý bằng GitLab CI/CD Variables hoặc hệ thống secret management.
- Không lưu mật khẩu, token hoặc khóa riêng tư trong repository.

## 13. Quản lý Version và Git Tag

Khuyến nghị áp dụng Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

Ví dụ:

| Phiên bản | Ý nghĩa |
|---|---|
| `1.0.0` | Phiên bản phát hành đầu tiên |
| `1.1.0` | Bổ sung chức năng tương thích |
| `1.1.1` | Sửa lỗi |
| `2.0.0` | Thay đổi lớn, có thể không tương thích |

Quy tắc:

- Tag phát hành có tiền tố `v`, ví dụ `v1.0.0`.
- Không di chuyển hoặc ghi đè tag đã phát hành.
- Mỗi tag Production phải liên kết được với commit và artifact triển khai.
- Ghi lại nội dung thay đổi trong Release Notes/Changelog.

## 14. Quy trình Rollback

Khi phiên bản triển khai phát sinh sự cố:

1. Xác định phiên bản/tag đang gặp lỗi.
2. Đánh giá mức độ ảnh hưởng và quyết định rollback.
3. Triển khai lại artifact của phiên bản ổn định trước đó.
4. Kiểm tra tính tương thích của Database.
5. Xác nhận dịch vụ hoạt động bình thường.
6. Ghi nhận sự cố và tạo bugfix/hotfix để xử lý triệt để.

**Lưu ý:** Rollback ứng dụng không đồng nghĩa với rollback Database. Các thay đổi schema cần được thiết kế theo hướng tương thích và có phương án xử lý dữ liệu riêng.

## 15. Checklist trước khi Merge

### Developer

- [ ] Đã cập nhật branch từ nguồn mới nhất.
- [ ] Code đã được format và lint.
- [ ] Build thành công.
- [ ] Unit Test đạt yêu cầu.
- [ ] Không có secret hoặc thông tin nhạy cảm trong commit.
- [ ] Đã cập nhật tài liệu/API nếu cần.
- [ ] Đã bổ sung script Database/migration nếu cần.
- [ ] Đã tự kiểm tra thay đổi và xử lý conflict.

### Reviewer / Maintainer

- [ ] Đúng branch đích.
- [ ] Nội dung thay đổi phù hợp với ticket.
- [ ] Không có thay đổi ngoài phạm vi không được giải thích.
- [ ] Đã review logic, bảo mật và hiệu năng phù hợp.
- [ ] Pipeline thành công.
- [ ] Có phương án xử lý rủi ro đối với thay đổi quan trọng.
- [ ] Đủ phê duyệt theo chính sách dự án.

## 16. Các nguyên tắc bắt buộc

1. Không commit trực tiếp lên `main`.
2. Không push trực tiếp lên branch được bảo vệ.
3. Mọi thay đổi phải có MR.
4. Không merge khi pipeline bắt buộc thất bại.
5. Không đưa secret vào Git.
6. Không tự ý sửa hoặc xóa tag đã phát hành.
7. Hotfix phải được đồng bộ về `develop`.
8. Sửa lỗi trên release branch phải được đồng bộ về `develop`.
9. Database script phải được quản lý và kiểm soát phiên bản.
10. Mỗi phiên bản Production phải truy vết được về source commit và artifact.

---

## 17. Phụ lục: Các lệnh Git thường dùng

### Lấy code mới nhất

```bash
git fetch origin
git checkout develop
git pull origin develop
```

### Tạo feature branch

```bash
git checkout -b feature/HNI-101-login-sso
git push -u origin feature/HNI-101-login-sso
```

### Commit và push

```bash
git add .
git commit -m "feat(auth): add Microsoft SSO login"
git push
```

### Cập nhật từ develop

```bash
git fetch origin
git merge origin/develop
```

### Xóa branch local sau khi merge

```bash
git branch -d feature/HNI-101-login-sso
```

### Xóa branch remote sau khi merge

```bash
git push origin --delete feature/HNI-101-login-sso
```

### Tạo release tag

```bash
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### Xem lịch sử commit

```bash
git log --oneline --graph --decorate --all
```

---

## 18. Lịch sử cập nhật tài liệu

| Phiên bản | Ngày | Nội dung | Người cập nhật |
|---|---|---|---|
| 1.0 | 2026-11-16 | Khởi tạo quy chuẩn Git Branch Strategy | Project Team |

---

**Kết thúc tài liệu.**
