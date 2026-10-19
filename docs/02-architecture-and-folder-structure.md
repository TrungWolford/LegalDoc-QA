# Kiến trúc hệ thống và cấu trúc thư mục

## 1. Nguyên tắc thiết kế

| Nguyên tắc | Lý do |
|---|---|
| **Modular monolith** (một Spring Boot app, chia module rõ ràng) | Nhóm nhỏ, 7 ngày, không cần microservice. Vẫn tách module để dễ đọc và test |
| **Layered architecture** (controller → service → repository) | Đúng với kinh nghiệm hiện có, dễ viết JUnit |
| **Pipeline một chiều cho phần phân tích** | Ingest → Normalize → Parse → Rules. Mỗi bước nhận input và trả output rõ ràng, test độc lập |
| **Rule là plugin** | Thêm rule = thêm một class `@Component`, không sửa code lõi |
| **Parser tách khỏi nguồn file** | POI và PDFBox cùng trả về `List<RawLine>`, phần còn lại không biết file gốc là gì |
| **Deterministic trước, AI sau** | AI chỉ là một `Rule` đặc biệt, bật/tắt được |
| **Mọi thay đổi trạng thái đi qua một service** | Đảm bảo state machine và audit không bị bypass |

## 2. Sơ đồ kiến trúc tổng thể

```
┌──────────────────────────────────────────────────────────────┐
│                         CLIENTS                              │
│  React SPA (Reviewer/Admin)     Trang trả lời (Counsel)      │
│                                  GitHub Action (CI, P2)      │
└──────────────┬───────────────────────────┬───────────────────┘
               │ REST + JWT                │ REST + token link
┌──────────────▼───────────────────────────▼───────────────────┐
│                   SPRING BOOT APPLICATION                    │
│                                                              │
│  ┌─────────────── API layer (controller) ────────────────┐   │
│  │ ContractController  FindingController  PackageController│  │
│  │ CounselPublicController  AdminController  AuthController│  │
│  └───────────────────────────┬───────────────────────────┘   │
│                              │                               │
│  ┌───────────────────────────▼───────────────────────────┐   │
│  │                  Application services                  │   │
│  │  ContractService   FindingWorkflowService              │   │
│  │  PackageService    VersionCompareService  ReportService│   │
│  └───────┬──────────────────────────────┬────────────────┘   │
│          │                              │                    │
│  ┌───────▼──────────────┐      ┌────────▼───────────────┐    │
│  │  ANALYSIS PIPELINE   │      │   COUNSEL WORKFLOW     │    │
│  │ 1 Ingest (POI/PDFBox)│      │  State machine         │    │
│  │ 2 Normalize          │      │  Question generator    │    │
│  │ 3 Parse → ClauseTree │      │  Token service (Redis) │    │
│  │ 4 Extract (terms,    │      │  Notification (SendGrid)│   │
│  │   refs, values)      │      │  Audit                 │    │
│  │ 5 Rule Engine        │      └────────────────────────┘    │
│  │   ├ ReferenceRules   │                                    │
│  │   ├ DefinitionRules  │                                    │
│  │   ├ ValueRules       │                                    │
│  │   ├ StructureRules   │                                    │
│  │   └ AiRule (P2)      │                                    │
│  └───────┬──────────────┘                                    │
│          │                                                   │
│  ┌───────▼──────────────────────────────────────────────┐    │
│  │        Persistence (Spring Data JPA + Flyway)         │    │
│  └───────┬──────────────────────────────┬───────────────┘    │
└──────────┼──────────────────────────────┼────────────────────┘
           │                              │
   ┌───────▼────────┐            ┌────────▼────────┐    ┌────────────┐
   │  PostgreSQL 16 │            │    Redis 7      │    │ File store │
   │  (dữ liệu)     │            │ token, rate limit│   │ (uploads)  │
   └────────────────┘            └─────────────────┘    └────────────┘

   Dịch vụ ngoài: SendGrid (email), Gemini API (plugin AI, P2)
```

## 3. Các thành phần chính

### 3.1 Analysis pipeline

```
File ──▶ DocumentReader ──▶ List<RawLine> ──▶ TextNormalizer ──▶ ClauseTreeBuilder
                                                                        │
                                                                        ▼
                                  RuleEngine ◀── ContractModel ◀── Extractors
                                      │          (tree + terms + refs + values)
                                      ▼
                               List<Finding>
```

| Thành phần | Trách nhiệm | Input → Output |
|---|---|---|
| `DocumentReader` (interface) | Đọc file theo định dạng. `DocxReader` dùng POI, `PdfReader` dùng PDFBox | `Path` → `List<RawLine>` (text, page, numbering gốc) |
| `TextNormalizer` | Chuẩn hóa ký tự, nối từ ngắt dòng, lọc header/footer | `List<RawLine>` → `List<RawLine>` |
| `ClauseTreeBuilder` | Nhận diện Article / Section / Clause bằng regex, dựng cây, báo dòng không parse được | `List<RawLine>` → `ClauseTree` + `ParseReport` |
| `TermExtractor` | Lấy defined terms từ Article Definitions, đếm số lần dùng | `ClauseTree` → `List<DefinedTerm>` |
| `ReferenceExtractor` | Tìm "Section 7.4", "5.1(b)" | `ClauseTree` → `List<CrossReference>` |
| `ValueExtractor` | Tìm tiền, %, ngày, số bằng chữ, gắn với thuật ngữ đi kèm | `ClauseTree` → `List<ExtractedValue>` |
| `ContractModel` | Gói tất cả kết quả trên, là input duy nhất của rule | — |
| `RuleEngine` | Lấy mọi `Rule` đang bật (đọc `rule_config`), chạy, gom finding, tính `fingerprint` | `ContractModel` → `List<Finding>` |

Interface rule:

```java
public interface Rule {
    String id();                         // "REF_BROKEN", "VALUE_CONFLICT", ...
    Severity defaultSeverity();
    List<Finding> check(ContractModel model);
}
```

### 3.2 Counsel workflow

| Thành phần | Trách nhiệm |
|---|---|
| `FindingWorkflowService` | Cổng duy nhất để đổi trạng thái finding. Kiểm tra chuyển trạng thái hợp lệ, ghi `finding_event`, dùng `@Version` chống ghi đè |
| `QuestionGenerator` | Sinh câu hỏi và đáp án gợi ý từ template theo `rule_id` |
| `PackageService` | Tạo gói, gửi, gửi lại, nhận câu trả lời, cập nhật finding |
| `CounselTokenService` | Sinh token ngẫu nhiên (≥ 32 byte), lưu Redis kèm TTL, chỉ lưu hash |
| `NotificationService` | Gửi email qua SendGrid (interface để mock khi test) |
| `VersionCompareService` | Đối chiếu finding giữa hai version bằng `fingerprint`: RESOLVED / STILL_OPEN / NEW |

### 3.3 Phân quyền và bảo mật

| Khu vực | Cơ chế |
|---|---|
| API Reviewer / Admin | Spring Security + JWT (Bearer), role `REVIEWER`, `ADMIN`, mật khẩu BCrypt |
| Trang luật sư (`/public/counsel/**`) | Không đăng nhập. Token ngẫu nhiên dùng một gói, TTL 7 ngày, lưu hash, rate limit theo IP trong Redis |
| Upload file | Giới hạn dung lượng, kiểm tra MIME + đuôi file, lưu tên ngẫu nhiên (UUID), không bao giờ thực thi nội dung file |
| Dữ liệu hợp đồng | Chỉ dùng hợp đồng mẫu, không đưa dữ liệu thật vào demo |

### 3.4 Plugin AI (P2)

`AiAmbiguityRule implements Rule`, bật bằng `rule_config.enabled`. Quy trình: gửi từng điều khoản cho Gemini, yêu cầu trả JSON `{clauseId, quote, issue}`, sau đó **validator kiểm tra `quote` có tồn tại nguyên văn trong điều khoản** (không thì loại bỏ) rồi mới tạo finding với `severity = INFO` và nhãn `source = AI`.

## 4. REST API

### Reviewer / Admin (JWT)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/auth/login` | Đăng nhập, trả JWT |
| POST | `/api/contracts` | Tạo hợp đồng + upload version đầu (multipart) |
| GET | `/api/contracts` | Danh sách hợp đồng |
| GET | `/api/contracts/{id}` | Chi tiết + danh sách version |
| POST | `/api/contracts/{id}/versions` | Upload version mới |
| GET | `/api/versions/{vid}/text` | Cây điều khoản và text để hiển thị |
| GET | `/api/versions/{vid}/findings` | Danh sách finding (lọc `severity`, `rule`, `status`) |
| GET | `/api/versions/{vid}/compare/{otherVid}` | So sánh finding giữa hai version |
| PATCH | `/api/findings/{id}/status` | Đổi trạng thái (body: `toStatus`, `note`, `version`) |
| GET | `/api/findings/{id}/events` | Audit trail của finding |
| POST | `/api/packages` | Tạo gói câu hỏi từ danh sách finding |
| POST | `/api/packages/{id}/send` | Gửi email cho luật sư |
| POST | `/api/packages/{id}/resend` | Sinh token mới và gửi lại |
| GET | `/api/versions/{vid}/report?format=csv\|pdf` | Xuất báo cáo (P2) |
| GET / PUT | `/api/admin/rules` | Xem và cấu hình rule (P2) |
| GET | `/api/admin/dashboard` | Số liệu thống kê (P2) |

### Counsel (public, token)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/public/counsel/{token}` | Lấy gói câu hỏi + đoạn trích hợp đồng liên quan |
| POST | `/public/counsel/{token}/answers` | Gửi câu trả lời (cho phép trả lời một phần) |

## 5. Luồng chính dạng sequence

### 5.1 Upload và phân tích

```
Reviewer → ContractController : POST /contracts (file)
ContractController → ContractService : create(file)
ContractService → Storage : save(file)
ContractService → DB : insert contract_version (ANALYZING)
ContractService → AnalysisPipeline : run(versionId)     // đồng bộ cho MVP, @Async nếu file lớn
AnalysisPipeline → DocumentReader → Normalizer → TreeBuilder → Extractors
AnalysisPipeline → RuleEngine : run(model)
AnalysisPipeline → DB : insert clause, defined_term, reference, extracted_value, finding
AnalysisPipeline → DB : update version status = REVIEW_READY
ContractController → Reviewer : 201 { versionId, findingCount }
```

### 5.2 Gửi câu hỏi và nhận trả lời

```
Reviewer → PackageController : POST /packages { findingIds, counselEmail }
PackageService → QuestionGenerator : build questions
PackageService → DB : insert question_package (DRAFT), question_item[]
Reviewer → PackageController : POST /packages/{id}/send
PackageService → CounselTokenService : issue(packageId, ttl=7d)   // Redis
PackageService → NotificationService : email(link)
PackageService → FindingWorkflowService : move findings → SENT_TO_COUNSEL
Counsel → PublicController : POST /public/counsel/{token}/answers
PackageService → DB : save answers
PackageService → FindingWorkflowService : move findings → RESOLVED / DISMISSED
PackageService → NotificationService : báo cho reviewer
```

## 6. Quyết định kỹ thuật quan trọng

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| Phân tích đồng bộ hay bất đồng bộ | Đồng bộ cho MVP | Hợp đồng vài chục trang xử lý dưới vài giây. Giữ code đơn giản, có thể thêm `@Async` sau |
| Lưu cây điều khoản ở DB hay chỉ trong bộ nhớ | Lưu DB (`clause`) | UI cần hiển thị văn bản, highlight theo offset và so sánh version mà không phải parse lại |
| Nhận diện "cùng một lỗi" giữa version | `fingerprint` = hash(rule_id + section_label + evidence đã chuẩn hóa) | So sánh v1 và v2 mà không cần ID cố định |
| Chống ghi đè đồng thời | JPA `@Version` trên `finding` | Hai reviewer sửa cùng lúc thì người sau nhận lỗi và tải lại |
| Token luật sư | Lưu hash trong DB, token thật chỉ có trong email, TTL trong Redis | Lộ DB cũng không lộ link |
| Frontend | React SPA riêng | Cần màn hình chia đôi, scroll sync. Nếu thiếu thời gian có thể thay bằng Thymeleaf + HTMX |
| Migration DB | Flyway | Schema có version, dựng lại môi trường bằng một lệnh |

## 7. Chiến lược test

| Loại | Công cụ | Phạm vi |
|---|---|---|
| Unit test rule | JUnit 5 | Mỗi rule ít nhất 1 case có lỗi và 1 case sạch, dùng `ContractModel` dựng tay |
| Unit test parser | JUnit 5 + file DOCX mẫu trong `src/test/resources` | Đúng số Article/Section, phát hiện dòng không parse được |
| Integration test | Testcontainers (PostgreSQL, Redis) | Upload → finding → gói câu hỏi → trả lời |
| State machine | JUnit 5 | Mọi chuyển hợp lệ và không hợp lệ |
| Benchmark | JUnit + runner riêng | Chạy bộ hợp đồng cài lỗi, xuất precision/recall theo rule |

## 8. Triển khai (Docker Compose)

```yaml
services:
  postgres:  # image postgres:16, volume pgdata
  redis:     # image redis:7
  backend:   # build ./backend, depends_on postgres + redis, env: DB_URL, REDIS_HOST, SENDGRID_KEY, JWT_SECRET
  frontend:  # build ./frontend, nginx phục vụ SPA, proxy /api sang backend
```

Chạy: `docker compose up --build`. Biến môi trường để trong `.env` (không commit), kèm `.env.example`.

## 9. Cấu trúc thư mục

### 9.1 Repo gốc

```
contract-compiler/
├── backend/                     # Spring Boot
├── frontend/                    # React + Vite
├── samples/                     # Hợp đồng mẫu + kết quả mong đợi
│   ├── contracts/
│   │   ├── lpa_clean.docx
│   │   ├── lpa_with_errors_v1.docx
│   │   └── lpa_with_errors_v2.docx
│   └── expected/
│       └── lpa_with_errors_v1.expected.json   # danh sách lỗi đã cài
├── docs/
│   ├── 01-system-overview.md
│   ├── 02-architecture-and-folder-structure.md
│   ├── 03-database-design.md
│   └── benchmark-report.md
├── .github/
│   └── workflows/
│       ├── ci.yml               # build + test
│       └── contract-lint.yml    # (P2) chạy linter trên PR
├── docker-compose.yml
├── .env.example
└── README.md
```

### 9.2 Backend

```
backend/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/contractcompiler/
    │   │   ├── ContractCompilerApplication.java
    │   │   │
    │   │   ├── common/                       # dùng chung
    │   │   │   ├── config/                   # SecurityConfig, RedisConfig, OpenApiConfig, AsyncConfig
    │   │   │   ├── exception/                # GlobalExceptionHandler, NotFoundException, InvalidTransitionException
    │   │   │   ├── security/                 # JwtService, JwtAuthFilter
    │   │   │   └── util/                     # TextUtils, HashUtils
    │   │   │
    │   │   ├── auth/                         # đăng nhập, user
    │   │   │   ├── AuthController.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── AppUser.java
    │   │   │   ├── AppUserRepository.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── contract/                     # hợp đồng + version
    │   │   │   ├── ContractController.java
    │   │   │   ├── ContractService.java
    │   │   │   ├── Contract.java
    │   │   │   ├── ContractVersion.java
    │   │   │   ├── ContractRepository.java
    │   │   │   ├── ContractVersionRepository.java
    │   │   │   ├── storage/                  # FileStorage (interface), LocalFileStorage
    │   │   │   └── dto/
    │   │   │
    │   │   ├── analysis/                     # pipeline phân tích
    │   │   │   ├── AnalysisPipeline.java     # điều phối các bước
    │   │   │   ├── ingest/
    │   │   │   │   ├── DocumentReader.java   # interface
    │   │   │   │   ├── DocxReader.java       # POI
    │   │   │   │   ├── PdfReader.java        # PDFBox
    │   │   │   │   └── RawLine.java
    │   │   │   ├── normalize/
    │   │   │   │   └── TextNormalizer.java
    │   │   │   ├── parse/
    │   │   │   │   ├── ClauseTreeBuilder.java
    │   │   │   │   ├── ClauseNode.java
    │   │   │   │   ├── ClauseTree.java
    │   │   │   │   ├── ParseReport.java
    │   │   │   │   └── Patterns.java         # regex tập trung
    │   │   │   ├── extract/
    │   │   │   │   ├── TermExtractor.java
    │   │   │   │   ├── ReferenceExtractor.java
    │   │   │   │   ├── ValueExtractor.java
    │   │   │   │   └── NumberWordsParser.java  # ICU4J
    │   │   │   ├── model/
    │   │   │   │   ├── ContractModel.java
    │   │   │   │   ├── DefinedTerm.java
    │   │   │   │   ├── CrossReference.java
    │   │   │   │   ├── ExtractedValue.java
    │   │   │   │   └── ValueType.java
    │   │   │   └── persistence/              # entity + repo cho clause, term, reference, value
    │   │   │
    │   │   ├── rules/                        # rule engine
    │   │   │   ├── Rule.java                 # interface
    │   │   │   ├── RuleEngine.java
    │   │   │   ├── RuleConfig.java           # entity
    │   │   │   ├── RuleConfigRepository.java
    │   │   │   ├── reference/
    │   │   │   │   ├── BrokenReferenceRule.java
    │   │   │   │   └── CircularReferenceRule.java    # P2, JGraphT
    │   │   │   ├── definition/
    │   │   │   │   ├── UndefinedTermRule.java
    │   │   │   │   ├── UnusedDefinitionRule.java
    │   │   │   │   └── DuplicateDefinitionRule.java
    │   │   │   ├── value/
    │   │   │   │   ├── DateLogicRule.java
    │   │   │   │   ├── WordsVsDigitsRule.java
    │   │   │   │   ├── ValueConflictRule.java
    │   │   │   │   └── PercentSumRule.java
    │   │   │   ├── structure/
    │   │   │   │   └── NumberingGapRule.java
    │   │   │   └── ai/                       # P2
    │   │   │       ├── AiAmbiguityRule.java
    │   │   │       ├── GeminiClient.java
    │   │   │       └── QuoteValidator.java
    │   │   │
    │   │   ├── finding/                      # finding + workflow
    │   │   │   ├── FindingController.java
    │   │   │   ├── FindingService.java
    │   │   │   ├── FindingWorkflowService.java   # state machine + audit
    │   │   │   ├── FindingStatus.java
    │   │   │   ├── Severity.java
    │   │   │   ├── Finding.java
    │   │   │   ├── FindingEvent.java
    │   │   │   ├── FindingRepository.java
    │   │   │   ├── FindingEventRepository.java
    │   │   │   ├── VersionCompareService.java
    │   │   │   └── dto/
    │   │   │
    │   │   ├── counsel/                      # gói câu hỏi cho luật sư
    │   │   │   ├── PackageController.java
    │   │   │   ├── CounselPublicController.java
    │   │   │   ├── PackageService.java
    │   │   │   ├── QuestionGenerator.java
    │   │   │   ├── CounselTokenService.java      # Redis
    │   │   │   ├── QuestionPackage.java
    │   │   │   ├── QuestionItem.java
    │   │   │   ├── repository/
    │   │   │   └── dto/
    │   │   │
    │   │   ├── notification/
    │   │   │   ├── NotificationService.java      # interface
    │   │   │   ├── SendGridNotificationService.java
    │   │   │   └── templates/
    │   │   │
    │   │   ├── report/                       # P2
    │   │   │   ├── ReportController.java
    │   │   │   ├── CsvReportService.java
    │   │   │   └── PdfReportService.java
    │   │   │
    │   │   └── admin/                        # P2
    │   │       ├── AdminController.java
    │   │       └── DashboardService.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-docker.yml
    │       ├── db/migration/                 # Flyway
    │       │   ├── V1__init_users.sql
    │       │   ├── V2__contract_and_structure.sql
    │       │   ├── V3__finding_and_audit.sql
    │       │   ├── V4__counsel_package.sql
    │       │   ├── V5__rule_config_and_seed.sql
    │       │   └── V6__benchmark.sql
    │       ├── question-templates.yml        # template câu hỏi theo rule_id
    │       └── email/counsel-request.html
    │
    └── test/
        ├── java/com/contractcompiler/
        │   ├── analysis/                     # test parser, extractor
        │   ├── rules/                        # mỗi rule một test class
        │   ├── finding/                      # test state machine
        │   ├── counsel/                      # test token, package
        │   ├── integration/                  # Testcontainers
        │   └── benchmark/                    # BenchmarkRunner
        └── resources/
            └── contracts/                    # DOCX dùng cho test
```

### 9.3 Frontend

```
frontend/
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── Dockerfile
├── nginx.conf
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/                          # client gọi backend
    │   ├── http.ts                   # axios + gắn JWT
    │   ├── contracts.ts
    │   ├── findings.ts
    │   ├── packages.ts
    │   └── counsel.ts
    ├── types/                        # kiểu TypeScript khớp DTO
    ├── pages/
    │   ├── LoginPage.tsx
    │   ├── ContractListPage.tsx      # danh sách + upload
    │   ├── ReviewPage.tsx            # màn hình chia đôi (trang chính)
    │   ├── ComparePage.tsx           # so sánh v1 vs v2
    │   ├── PackagePage.tsx           # soạn và gửi gói câu hỏi
    │   ├── CounselAnswerPage.tsx     # public, qua token
    │   ├── DashboardPage.tsx         # P2
    │   └── RulesAdminPage.tsx        # P2
    ├── components/
    │   ├── contract/
    │   │   ├── ContractViewer.tsx    # render text + <mark>
    │   │   ├── ClauseBlock.tsx
    │   │   └── UploadDropzone.tsx
    │   ├── finding/
    │   │   ├── FindingList.tsx
    │   │   ├── FindingCard.tsx
    │   │   ├── SeverityBadge.tsx
    │   │   ├── StatusBadge.tsx
    │   │   └── FindingTimeline.tsx   # audit trail
    │   ├── counsel/
    │   │   └── QuestionForm.tsx
    │   └── common/                   # Button, Modal, Toast, Layout
    ├── hooks/
    │   ├── useFindings.ts
    │   └── useScrollToClause.ts
    └── utils/
```

## 10. Quy ước code

| Chủ đề | Quy ước |
|---|---|
| Package | Chia theo module nghiệp vụ (`contract`, `finding`, `counsel`), không chia theo kiểu `controller/service` toàn cục |
| Phụ thuộc giữa module | `rules` chỉ phụ thuộc `analysis.model`. `finding` không phụ thuộc `counsel`. `counsel` gọi `FindingWorkflowService` |
| DTO | Controller không trả entity trực tiếp, dùng `record` DTO |
| Lỗi API | `GlobalExceptionHandler` trả JSON thống nhất `{code, message, details}` |
| Regex | Gom tất cả vào `Patterns.java`, mỗi regex có comment và test riêng |
| Rule ID | `UPPER_SNAKE_CASE`, ổn định, vì được lưu vào DB và `fingerprint` |
| Commit | Mỗi rule một commit kèm test |
