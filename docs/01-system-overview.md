# Contract Compiler + Counsel Workflow: Tổng quan hệ thống

> Topic: **The Legal Reviewer** (Anduin Transactions Challenge)
> Định vị: *"Chúng tôi coi hợp đồng như source code: compile, lint, và chỉ đưa luật sư những lỗi đã được chứng minh bằng bằng chứng."*

---

## 1. Bối cảnh và vấn đề

Anduin số hóa các hợp đồng quỹ đầu tư phức tạp (LPA, Subscription Agreement, Side Letter) thành form online, quy trình ký và dữ liệu có cấu trúc. Trước khi số hóa, đội ngũ phải đọc hợp đồng để tìm lỗi đầu vào: typo, tham chiếu thiếu, mâu thuẫn, thuật ngữ mơ hồ. Lỗi tìm ra phải được xác nhận với luật sư bên ngoài. Nếu bỏ sót, cả team làm việc trên input sai và phải làm lại.

**Giải pháp:** một hệ thống đọc hợp đồng, dựng cấu trúc điều khoản, chạy bộ rule xác định (deterministic) để phát hiện lỗi kèm bằng chứng, rồi quản lý toàn bộ vòng đời xử lý lỗi, từ phát hiện đến luật sư xác nhận.

## 2. Điểm khác biệt

| Điểm | Mô tả |
|---|---|
| Coi hợp đồng như source code | Parser → AST → symbol table → rule engine. Lỗi báo như compiler, có vị trí và bằng chứng |
| Deterministic trước, AI sau | Lõi là rule chạy bằng code, kết quả lặp lại được và test bằng JUnit. AI (Gemini) chỉ là plugin tùy chọn |
| Mỗi lỗi có bằng chứng | Trích đúng đoạn gốc, số điều khoản, số trang. Reviewer kiểm chứng trong vài giây |
| Workflow hoàn chỉnh | Detected → Confirmed → Sent to counsel → Resolved, có audit trail |
| Vòng lặp theo phiên bản | Upload v2 thì hệ thống chỉ ra lỗi đã hết, lỗi còn lại và lỗi mới phát sinh |
| Có số liệu đánh giá | Bộ hợp đồng cài lỗi biết trước, đo precision/recall theo từng rule |

## 3. Phạm vi

**Trong phạm vi (MVP):** hợp đồng DOCX và PDF dạng text, tiếng Anh, format đánh số `ARTICLE n` / `n.m` / `(a)`.

**Ngoài phạm vi:** PDF scan (cần OCR), hợp đồng không đánh số điều khoản, tiếng Việt, tư vấn pháp lý (hệ thống chỉ phát hiện và đặt câu hỏi, không kết luận đúng sai).

## 4. Actors

| Actor | Vai trò |
|---|---|
| **Reviewer** (nhân viên Anduin) | Upload hợp đồng, xem lỗi, phân loại, tạo và gửi gói câu hỏi cho luật sư |
| **External Counsel** (luật sư ngoài) | Nhận email, mở link, trả lời từng câu hỏi (không cần tài khoản) |
| **Admin / Lead** | Bật/tắt rule, đổi severity, xem dashboard |
| **CI System** (tùy chọn) | Tự chạy kiểm tra khi hợp đồng thay đổi trong Git |

## 5. Input / Output

### Input

- File hợp đồng `.docx` (ưu tiên) hoặc `.pdf` dạng text.
- Tùy chọn: cấu hình rule (bật/tắt, severity, tham số).

Ví dụ nội dung hợp đồng rút gọn:

```
ARTICLE 1 - DEFINITIONS
1.1 "Fund" means Anduin Growth Fund I, L.P.
1.2 "Capital Commitment" means the amount an Investor agrees to contribute.
1.3 "Closing Date" means January 15, 2025.

ARTICLE 2 - FEES
2.1 The Management Fee is 2% per annum of the Capital Commitment.
2.2 The Management Fee payable by each Limited Partner is 2.5% per annum.
2.3 Carried Interest is 20%, subject to the Hurdle Rate in Section 7.4.

ARTICLE 3 - TERM
3.1 The Fund shall terminate on December 31, 2024.
3.2 The minimum Capital Commitment is One Hundred Thousand Dollars ($1,000,000).
```

### Output

**Output 1: báo cáo lỗi (finding list)**

| # | Severity | Loại lỗi | Vị trí | Bằng chứng | Lý do |
|---|---|---|---|---|---|
| 1 | Error | Tham chiếu gãy | 2.3 | "subject to the Hurdle Rate in Section 7.4" | Không có Section 7.4 |
| 2 | Error | Ngày sai logic | 3.1 vs 1.3 | 31/12/2024 vs 15/01/2025 | Quỹ kết thúc trước khi thành lập |
| 3 | Error | Số chữ ≠ số | 3.2 | "One Hundred Thousand" vs "$1,000,000" | 100.000$ ≠ 1.000.000$ |
| 4 | Error | Mâu thuẫn con số | 2.1 vs 2.2 | 2% vs 2.5% | Cùng Management Fee, hai mức |
| 5 | Warning | Thuật ngữ chưa định nghĩa | 2.2 | "Limited Partner" | Dùng nhưng không có định nghĩa |
| 6 | Info | Định nghĩa không dùng | 1.1 | "Fund" | Định nghĩa nhưng không được dùng |

**Output 2: gói câu hỏi gửi luật sư (email + trang trả lời)**

> 1. Section 2.3 tham chiếu Section 7.4 (Hurdle Rate) nhưng không tồn tại. Thiếu điều khoản hay đánh số sai?
> 2. Management Fee là 2% (2.1) hay 2.5% (2.2)?
> 3. Minimum Capital Commitment là $100,000 hay $1,000,000?
> 4. Ngày kết thúc quỹ (31/12/2024) đứng trước Closing Date (15/01/2025). Ngày nào đúng?

**Output 3:** trạng thái từng lỗi theo thời gian (audit trail), báo cáo CSV/PDF, so sánh giữa các phiên bản (v1 vs v2), dashboard thống kê.

## 6. Tech stack tổng

| Tầng | Công nghệ |
|---|---|
| Backend | Java 17, Spring Boot 3 (Web, Data JPA, Security, Validation) |
| Đọc tài liệu | Apache POI (DOCX), Apache PDFBox (PDF) |
| Phân tích | Java regex, `java.time`, ICU4J (số chữ → số), JGraphT (vòng tham chiếu, P2) |
| Database | PostgreSQL 16, Flyway (migration) |
| Cache / token | Redis 7 (token link luật sư, rate limit) |
| Email | SendGrid |
| Frontend | React + Vite + Tailwind (PDF.js, Cytoscape.js, Recharts ở P2) |
| API docs | Swagger / OpenAPI (springdoc) |
| Test | JUnit 5, Testcontainers, Mockito |
| DevOps | Docker, Docker Compose, GitHub Actions |
| AI (plugin, P2) | Gemini API, output JSON, validator "quote phải tồn tại trong văn bản gốc" |

## 7. Workflow end-to-end

```
[Reviewer]                     [Hệ thống]                          [Counsel]
    │                               │                                  │
 1. Upload ───────────────▶ 2. Ingest (POI/PDFBox)                     │
    │                        3. Normalize + Parse → cây điều khoản     │
    │                        4. Chạy Rule Engine → Finding             │
 5. Xem kết quả ◀────────── 5. Lưu DB, trả danh sách lỗi               │
 6. Triage: Confirm / Dismiss                                          │
 7. Chọn lỗi → Tạo gói câu hỏi ─▶ 8. Gom câu hỏi, sinh token (Redis)   │
    │                        9. Gửi email (SendGrid) ─────────────────▶ 10. Mở link
    │                                                                  11. Trả lời từng câu
 12. Nhận kết quả ◀───────── 11. Cập nhật trạng thái + audit log ◀─────┘
 13. Sửa hợp đồng, upload v2 ──▶ 14. Chạy lại, so sánh v1 vs v2
 15. Xuất báo cáo / đóng hợp đồng
```

### Giai đoạn A: Phân tích (tự động)

| Bước | Việc xảy ra | Output |
|---|---|---|
| 1. Upload | Kiểm tra định dạng và dung lượng, lưu file, tạo `contract_version` (status `ANALYZING`) | `versionId` |
| 2. Ingest | DOCX: đọc paragraph kèm numbering. PDF: đọc theo trang, lọc header/footer, nối từ ngắt dòng | Dòng văn bản kèm số trang |
| 3. Normalize | Chuẩn hóa ngoặc kép cong, gạch ngang dài, khoảng trắng | Text sạch |
| 4. Parse | Regex dựng cây `Article → Section → Clause`, trích defined terms, cross-references, giá trị (tiền, %, ngày) | `ContractTree` |
| 5. Rule engine | Chạy tất cả `Rule` đang bật, mỗi rule trả `List<Finding>` | Danh sách finding |
| 6. Lưu | Lưu finding, đổi status `REVIEW_READY`, ghi audit | Bản ghi DB |

Nếu parser không nhận ra cấu trúc, hệ thống báo rõ "không parse được N dòng" thay vì im lặng trả về 0 lỗi.

### Giai đoạn B: Reviewer triage

| Bước | Việc xảy ra | Trạng thái |
|---|---|---|
| 7. Xem kết quả | Màn hình chia đôi, click lỗi thì cuộn và highlight | `DETECTED` |
| 8. Phân loại | **Dismiss** (báo nhầm, ghi lý do) hoặc **Confirm** (lỗi thật) | `DISMISSED` / `CONFIRMED` |
| 9. Cần hỏi luật sư? | Lỗi chắc chắn thì tự xử lý. Lỗi cần phán đoán pháp lý thì đánh cờ `needs_counsel` | `CONFIRMED` |

### Giai đoạn C: Hỏi luật sư

| Bước | Việc xảy ra |
|---|---|
| 10. Tạo gói | Chọn nhiều lỗi, hệ thống sinh câu hỏi từ template theo loại lỗi, reviewer sửa nếu cần |
| 11. Gửi | Sinh token ngẫu nhiên (TTL 7 ngày) lưu Redis, gửi email có link. Finding chuyển `SENT_TO_COUNSEL` |
| 12. Trả lời | Luật sư mở link (không cần đăng nhập), chọn đáp án cho từng câu kèm ghi chú |
| 13. Ghi nhận | Finding chuyển `RESOLVED` (hoặc `DISMISSED` nếu luật sư nói không phải lỗi), ghi audit, báo lại reviewer |

### Giai đoạn D: Khép vòng

| Bước | Việc xảy ra |
|---|---|
| 14. Upload bản sửa | Reviewer upload v2 |
| 15. So sánh | Phân tích v2, đối chiếu v1 qua `fingerprint`: lỗi đã hết, lỗi còn, lỗi mới phát sinh |
| 16. Đóng | Không còn lỗi mở thì hợp đồng chuyển `READY_FOR_DIGITIZATION`, xuất báo cáo |

### Máy trạng thái của Finding

```
                 ┌──────────────▶ DISMISSED (báo nhầm)
                 │
DETECTED ──▶ CONFIRMED ──▶ SENT_TO_COUNSEL ──▶ RESOLVED
                 │                 │
                 │                 └──▶ DISMISSED (luật sư nói không phải lỗi)
                 └──▶ RESOLVED (tự sửa, không cần hỏi luật sư)

Quay lại: DISMISSED ──▶ DETECTED (reviewer đổi ý)
```

Service chặn mọi bước chuyển không hợp lệ. Mỗi lần chuyển ghi `finding_event` (ai, khi nào, từ đâu sang đâu, lý do).

### Ngoại lệ cần xử lý

| Tình huống | Cách xử lý |
|---|---|
| Không parse được cấu trúc | Báo số dòng không nhận ra, cho xem text thô |
| PDF scan | Báo "chưa hỗ trợ file scan", không chạy rule |
| Token hết hạn | Trang báo hết hạn, reviewer bấm "Gửi lại" (token mới, token cũ vô hiệu) |
| Luật sư trả lời một phần | Câu đã trả lời → `RESOLVED`, còn lại giữ `SENT_TO_COUNSEL`, package → `PARTIAL` |
| Dismiss nhầm | Cho đổi về `DETECTED`, có audit |
| Hai reviewer sửa cùng finding | Optimistic locking (`@Version`) |

## 8. Use case

| ID | Actor | Use case | Priority |
|---|---|---|---|
| UC1 | Reviewer | Upload hợp đồng | P0 |
| UC2 | Reviewer | Xem kết quả phân tích (lỗi theo severity, có bằng chứng) | P0 |
| UC3 | Reviewer | Xem lỗi trong ngữ cảnh (click → cuộn và highlight) | P0 |
| UC4 | Reviewer | Phân loại lỗi (Confirm / Dismiss) | P0 |
| UC5 | Reviewer | Tạo gói câu hỏi cho luật sư | P0 |
| UC6 | Reviewer | Gửi gói câu hỏi qua email | P1 |
| UC7 | Counsel | Trả lời câu hỏi qua link | P1 |
| UC8 | Reviewer | Theo dõi trạng thái và lịch sử lỗi | P1 |
| UC9 | Reviewer | Upload phiên bản mới và so sánh với phiên bản trước | P1 |
| UC10 | Reviewer | Xuất báo cáo PDF/CSV | P2 |
| UC11 | Admin | Cấu hình rule | P2 |
| UC12 | Admin | Xem dashboard | P2 |
| UC13 | CI System | Kiểm tra tự động khi push/PR | P2 |

## 9. Danh sách feature theo priority

**P0 = bắt buộc cho MVP. P1 = nên có. P2 = điểm cộng, chỉ chọn 1-2 thứ.**

### Module A: Nhập tài liệu

| Feature | Pri | Tech |
|---|---|---|
| Upload DOCX | P0 | Spring `MultipartFile`, local disk/MinIO |
| Đọc DOCX kèm numbering | P0 | Apache POI `XWPFDocument` |
| Chuẩn hóa text | P0 | Java thuần |
| Upload và đọc PDF text | P1 | Apache PDFBox |
| Lọc header/footer, nối từ ngắt dòng | P1 | Java thuần |

### Module B: Parser

| Feature | Pri | Tech |
|---|---|---|
| Dựng cây điều khoản | P0 | Regex, `ClauseNode` |
| Trích defined terms | P0 | Regex |
| Trích cross-reference | P0 | Regex |
| Trích giá trị (tiền, %, ngày, số bằng chữ) | P0 | Regex, `java.time`, ICU4J |
| Parse ngày đa format | P2 | Natty |

### Module C: Rule engine

| Feature | Pri | Tech |
|---|---|---|
| Khung `Rule` + `Finding`, tự đăng ký | P0 | Spring `@Component` |
| Rule: tham chiếu gãy | P0 | `Set<String>` |
| Rule: term chưa định nghĩa / không dùng | P0 | Regex, `Map` đếm |
| Rule: ngày sai logic | P0 | `LocalDate` |
| Rule: số chữ ≠ số | P0 | ICU4J `RuleBasedNumberFormat` |
| Rule: mâu thuẫn con số cùng khoản mục | P0 | Nhóm theo term, `BigDecimal` |
| Rule: định nghĩa trùng nghĩa khác nhau | P1 | `Map<Term, List<Definition>>` |
| Rule: tổng phần trăm ≠ 100% | P1 | `BigDecimal` |
| Rule: đánh số nhảy cách | P1 | Duyệt cây |
| Rule: vòng tham chiếu | P2 | JGraphT |
| Plugin AI: điều khoản mơ hồ | P2 | Gemini + validator quote |

### Module D: Finding và báo cáo

| Feature | Pri | Tech |
|---|---|---|
| Lưu finding | P0 | PostgreSQL, JPA |
| API danh sách finding, lọc | P0 | REST, Swagger |
| So sánh finding giữa các version | P1 | `fingerprint` |
| Xuất CSV | P2 | OpenCSV |
| Xuất PDF | P2 | OpenPDF / Thymeleaf + Flying Saucer |

### Module E: Counsel workflow

| Feature | Pri | Tech |
|---|---|---|
| State machine finding | P0 | Java enum + service |
| Gom finding thành gói câu hỏi | P0 | Service, template câu hỏi |
| Gửi email | P1 | SendGrid |
| Trang trả lời qua token | P1 | Redis (TTL), REST |
| Audit trail | P1 | Bảng `finding_event` |
| Gán người xử lý, comment | P2 | JPA |

### Module F: Giao diện

| Feature | Pri | Tech |
|---|---|---|
| Trang upload + danh sách hợp đồng | P0 | React + Vite + Tailwind |
| Màn hình chia đôi (văn bản / lỗi) | P0 | React, `<mark>` |
| Click lỗi → cuộn đến câu | P1 | `scrollIntoView` |
| Highlight trên PDF gốc | P2 | PDF.js + tọa độ PDFBox |
| Đồ thị điều khoản | P2 | Cytoscape.js |
| Dashboard | P2 | Recharts |

### Module G: Chất lượng và vận hành

| Feature | Pri | Tech |
|---|---|---|
| Unit test từng rule | P0 | JUnit 5 |
| Docker Compose một lệnh | P0 | Docker Compose |
| Bộ hợp đồng mẫu cài lỗi | P0 | 3 file DOCX |
| Benchmark precision/recall | P1 | JUnit + script |
| Đăng nhập Reviewer/Admin | P1 | Spring Security + JWT |
| Mutation: tự sinh biến thể lỗi | P2 | POI |
| GitHub Action chạy linter | P2 | GitHub Actions + Docker |
| Rule cấu hình bằng YAML | P2 | SnakeYAML |

## 10. Kế hoạch 7 ngày

| Ngày | Việc | Pri |
|---|---|---|
| 1 | Setup project, Docker Compose, soạn 3 hợp đồng mẫu, Flyway schema | P0 |
| 2 | Upload DOCX, đọc POI, dựng cây điều khoản | P0 |
| 3 | Trích term/reference/giá trị; rule tham chiếu gãy, term | P0 |
| 4 | Rule ngày, số chữ ≠ số, mâu thuẫn con số; JUnit | P0 |
| 5 | API finding, state machine, gom câu hỏi, UI chia đôi | P0 |
| 6 | SendGrid, trang luật sư, audit, so sánh version, PDF input | P1 |
| 7 | Benchmark, README, video demo, thêm 1 thứ P2 (khuyên GitHub Action) | P1/P2 |

**Quy tắc cắt scope:** hết ngày 5 mà P0 chưa xong thì bỏ PDF và email, ưu tiên demo trọn luồng upload → lỗi → câu hỏi luật sư.

## 11. Kịch bản demo 3 phút

| Thời gian | Nội dung |
|---|---|
| 0:00–0:30 | Vấn đề: hợp đồng lỗi thì số hóa phải làm lại |
| 0:30–1:15 | Upload v1, hiện 6 lỗi, click lỗi để highlight |
| 1:15–1:45 | Triage, tạo gói 4 câu hỏi, gửi email |
| 1:45–2:15 | Vai luật sư, trả lời qua link |
| 2:15–2:45 | Upload v2, thấy lỗi đã hết và lỗi mới |
| 2:45–3:00 | Số liệu benchmark: bắt được X% trên bộ mẫu |

## 12. Tiêu chí thành công

- Bắt đúng ≥ 90% lỗi trên bộ mẫu cài lỗi biết trước, false positive thấp (báo trong benchmark).
- Mỗi finding có bằng chứng kiểm chứng được.
- Chạy toàn bộ hệ thống bằng `docker compose up`.
- Demo trọn vòng: upload → lỗi → hỏi luật sư → trả lời → upload v2.
