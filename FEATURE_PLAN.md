# JobTracker — план реалізації трьох фіч (BACKEND)

> Складено на основі реального коду репозиторію. Рішення узгоджені:
> 1. **Per-application CV** — годує AI-фічі цієї заявки, файл зберігається як `bytea` в Postgres.
> 2. **Кастомні статуси** — per-user таблиця замість enum, вільний рух між колонками, ретайр state machine.
> 3. **Архів** — розведено з видаленням: `DELETE` = hard delete, окремі archive/unarchive + перегляд + restore.
>
> **Фронтенд реалізується напряму в коді (`frontend/`)** — у цьому документі лише бекенд.
> Фронт-контракти (форми відповідей/запитів), на які орієнтується UI, зафіксовані нижче в DTO кожної фічі.

---

## 0. Перед стартом (обов'язково)

Фіча 2 роздуває diff по багатьох файлах. Поки не полагоджено git-гігієну з `KNOWN_ISSUES.md`, ревʼю змін буде неможливим:

- **P0 з KNOWN_ISSUES:** полагодити CRLF/LF (`git add --renormalize .` окремим комітом), додати root `.gitignore`, підтягнути локальний `main`. Тільки після цього починати фічі.
- Працюємо в `develope`, кожна фіча — окрема гілка від `develope`, PR у `develope` → merge у `main`.

### Рекомендований порядок і гілки

| # | Фіча | Гілка | Flyway | Складність | Чому такий порядок |
|---|---|---|---|---|---|
| 1 | Архів | `feat/archive` | — (міграції не треба) | S | Бекенд ~70% готовий, лише wiring. Розминка. |
| 2 | Per-app CV | `feat/per-application-cv` | `V18` | M | Адитивна, ізольована, нічого не ламає. |
| 3 | Кастомні статуси | `feat/custom-statuses` | `V19`–`V21` | L | Великий рефактор, робимо останнім із повною увагою. |

Фіча 3 автоматично закриває з KNOWN_ISSUES: **5.6** (self-loop на фронті), **5.7** (приховані WITHDRAWN/GHOSTED) і ретайрить state machine.

---

# Фіча архів: розвести архів і видалення

**Що вже є:** `Application.archived` (boolean), `ApplicationService.archive()/unarchive()/hardDelete()`, `ApplicationSpecifications.notArchived()`. Не вистачає: endpoint'ів archive/unarchive, фільтра для перегляду архіву, а `DELETE` зараз хибно робить soft-archive.

### Крок 1.1 — Контролер: розвести DELETE і archive

`application/ApplicationController.java`:

```java
// DELETE тепер СПРАВЖНЄ видалення (було: archive)
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id) {
    applicationService.hardDelete(principal.user().getId(), id);
}

@PostMapping("/{id}/archive")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void archive(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id) {
    applicationService.archive(principal.user().getId(), id);
}

@PostMapping("/{id}/unarchive")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void unarchive(
        @AuthenticationPrincipal CustomUserDetails principal,
        @PathVariable UUID id) {
    applicationService.unarchive(principal.user().getId(), id);
}
```

Сервісні методи `archive/unarchive/hardDelete` вже існують — не чіпаємо.

### Крок 1.2 — Фільтр `archived` у списку

`application/dto/ApplicationFilters.java` — додати поле:

```java
private Boolean archived; // null/false → активні; true → архів
```

`application/dto/ApplicationSpecifications.java` — замінити жорсткий `notArchived()` на параметризований:

```java
/** archived=null|false → приховати архів; archived=true → показати ЛИШЕ архів. */
public static Specification<Application> archivedFilter(Boolean archived) {
    return (root, query, cb) ->
        cb.equal(root.get("archived"), Boolean.TRUE.equals(archived));
}
```

`ApplicationService.list(...)` — замінити `.and(notArchived())` на `.and(archivedFilter(f.getArchived()))`.

**Фронт-контракт:** `GET /api/v1/applications?archived=true` → сторінка архівних; `POST /{id}/archive`, `POST /{id}/unarchive` → 204; `DELETE /{id}` → 204 (hard).

### Крок 1.3 — Тести

`ApplicationControllerIT`: `DELETE` → 204 і запис зникає з БД; `POST /archive` → 204 + `archived=true`; список без параметра не містить архівних; `?archived=true` — лише архівні; `POST /unarchive` повертає в активні.

---

# Фіча per-app CV: своє CV на кожну вакансію

**Мета:** до заявки прикріплюється PDF/DOCX; зберігається як `bytea`, можна скачати, а його текст стає контекстом для AI цієї заявки (cover letter / tailored CV / interview prep) замість Master CV. **Не плутати з Tailored CV** (той — LLM-генерований).

### Крок 2.1 — Міграція `V18__create_application_cvs.sql`

Байти в окремій таблиці, щоб не тягнути `bytea` у кожен `SELECT` заявки. 1 CV на заявку — новий upload замінює старий.

```sql
CREATE TABLE application_cvs (
    id              UUID         PRIMARY KEY,
    application_id  UUID         NOT NULL UNIQUE,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size       BIGINT       NOT NULL,
    file_bytes      BYTEA        NOT NULL,
    extracted_text  TEXT,                       -- Tika-текст для AI-контексту
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_application_cvs_application
        FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,

    CONSTRAINT check_application_cvs_content_type
        CHECK (content_type IN (
            'application/pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        ))
);

CREATE INDEX idx_application_cvs_application ON application_cvs (application_id);
```

### Крок 2.2 — Entity `application/cv/ApplicationCv.java`

```java
@Entity
@Table(name = "application_cvs")
@Getter @Setter
public class ApplicationCv {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(name = "file_name", nullable = false)   private String fileName;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "file_size", nullable = false)    private long fileSize;

    // LAZY: байти не тягнемо, поки явно не звернемось (тільки на download).
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_bytes", nullable = false)   private byte[] fileBytes;

    @Column(name = "extracted_text", columnDefinition = "TEXT") private String extractedText;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false)                    private Instant updatedAt;

    @PrePersist void onCreate() { var n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
```

### Крок 2.3 — Repository `application/cv/ApplicationCvRepository.java`

```java
public interface ApplicationCvRepository extends JpaRepository<ApplicationCv, UUID> {

    Optional<ApplicationCv> findByApplicationId(UUID applicationId);
    boolean existsByApplicationId(UUID applicationId);
    void deleteByApplicationId(UUID applicationId);

    // Легка проекція без байтів — для метаданих і AI-контексту.
    @Query("""
        select c.id as id, c.fileName as fileName, c.contentType as contentType,
               c.fileSize as fileSize, c.extractedText as extractedText,
               c.createdAt as createdAt
        from ApplicationCv c
        where c.application.id = :applicationId
        """)
    Optional<ApplicationCvMeta> findMetaByApplicationId(UUID applicationId);
}
```

`ApplicationCvMeta` — Spring Data projection interface (getId/getFileName/getContentType/getFileSize/getExtractedText/getCreatedAt). Байти в неї не входять → жоден метадата-запит не витягує `bytea`.

### Крок 2.4 — Service `application/cv/ApplicationCvService.java`

Валідацію переюзовуємо з наявної `cv/CvFileValidation`, парсинг тексту — з `cv/CvFileParser` (Tika).

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationCvService {

    private final ApplicationCvRepository repository;
    private final ApplicationRepository applicationRepository;
    private final CvFileParser fileParser;       // існує (Tika)
    private final CvFileValidation validation;    // існує

    @Transactional
    public ApplicationCvMetaResponse upload(UUID userId, UUID applicationId, MultipartFile file) {
        Application app = fetchOwned(userId, applicationId);
        validation.validate(file); // тип/розмір

        String text;
        try { text = fileParser.extractText(file); }         // Tika → plain text
        catch (Exception e) { throw new CvExtractionException("Could not read the uploaded CV file."); }

        ApplicationCv cv = repository.findByApplicationId(applicationId).orElseGet(ApplicationCv::new);
        cv.setApplication(app);
        cv.setFileName(file.getOriginalFilename());
        cv.setContentType(file.getContentType());
        cv.setFileSize(file.getSize());
        try { cv.setFileBytes(file.getBytes()); }
        catch (IOException e) { throw new CvExtractionException("Could not store the uploaded CV file."); }
        cv.setExtractedText(text);

        return toMeta(repository.save(cv));
    }

    public ApplicationCvMetaResponse getMeta(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        return repository.findByApplicationId(applicationId).map(this::toMeta)
                .orElseThrow(() -> new ResourceNotFoundException("No CV attached to application: " + applicationId));
    }

    public ApplicationCv getForDownload(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        return repository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("No CV attached to application: " + applicationId));
    }

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        fetchOwned(userId, applicationId);
        repository.deleteByApplicationId(applicationId);
    }

    private Application fetchOwned(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    private ApplicationCvMetaResponse toMeta(ApplicationCv cv) {
        ApplicationCvMetaResponse r = new ApplicationCvMetaResponse();
        r.setId(cv.getId()); r.setFileName(cv.getFileName()); r.setContentType(cv.getContentType());
        r.setFileSize(cv.getFileSize()); r.setCreatedAt(cv.getCreatedAt());
        return r;
    }
}
```

> Якщо `CvFileParser` не має публічного `extractText(MultipartFile)` — додати тонку обгортку над наявною Tika-логікою (вона вже є для Master CV upload).

DTO `application/cv/dto/ApplicationCvMetaResponse.java` — `@Getter @Setter`: `id, fileName, contentType, fileSize, createdAt` (без байтів і тексту).

### Крок 2.5 — Controller `application/cv/ApplicationCvController.java`

```java
@RestController
@RequestMapping("/api/v1/applications/{appId}/cv")
@RequiredArgsConstructor
public class ApplicationCvController {

    private final ApplicationCvService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationCvMetaResponse upload(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId,
            @RequestParam("file") MultipartFile file) {
        return service.upload(principal.user().getId(), appId, file);
    }

    @GetMapping
    public ApplicationCvMetaResponse getMeta(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        return service.getMeta(principal.user().getId(), appId);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        ApplicationCv cv = service.getForDownload(principal.user().getId(), appId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cv.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + cv.getFileName() + "\"")
                .body(cv.getFileBytes());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID appId) {
        service.delete(principal.user().getId(), appId);
    }
}
```

**Фронт-контракт:** `POST /applications/{appId}/cv` (multipart, поле `file`) → 201 + `{id,fileName,contentType,fileSize,createdAt}`; `GET` → те саме або 404; `GET /download` → байти; `DELETE` → 204.

### Крок 2.6 — Підключення до AI-контексту

`application/cv/CvContextResolver.java`:

```java
@Service
@RequiredArgsConstructor
public class CvContextResolver {

    private final ApplicationCvRepository appCvRepository;

    /** Текст прикріпленого до заявки CV, якщо є і не порожній. */
    public Optional<String> overrideText(UUID applicationId) {
        return appCvRepository.findMetaByApplicationId(applicationId)
                .map(ApplicationCvMeta::getExtractedText)
                .filter(t -> t != null && !t.isBlank());
    }
}
```

Приклад у `interview/InterviewPrepGenerator` (той самий патерн для `coverletter/CoverLetterGenerator` і `cv/tailored/TailoredCvGenerator`):

```java
private final CvContextResolver cvContextResolver; // інжект

public ParsedPrepGuide generate(UUID userId, Application app, MasterCv master) {
    String jobContext = buildJobContext(app);

    // Якщо до заявки прикріплено CV — беремо його текст, інакше структурований Master CV.
    String masterContext = cvContextResolver.overrideText(app.getId())
            .map(text -> "Candidate CV (tailored for this specific job):\n" + text)
            .orElseGet(() -> buildMasterContext(master));

    List<SimilarInterviewNote> relevantNotes = ragService.findRelevantPastNotes(userId, app);
    String pastNotesContext = InterviewPrepRagPrompt.formatPastNotes(relevantNotes);

    String prompt = InterviewPrepPrompt.build(jobContext, masterContext, pastNotesContext);
    String json = aiService.complete(prompt);
    if (json == null || json.isBlank())
        throw new BusinessRuleException("LLM returned empty response for interview prep");
    return parseJson(json);
}
```

У cover letter / tailored CV сервісах, де жорстка вимога Master CV — послабити до «attached CV **або** Master CV»:

```java
Optional<String> attached = cvContextResolver.overrideText(applicationId);
if (attached.isEmpty() && masterCvRepository.findByUserId(userId).isEmpty()) {
    throw new BusinessRuleException("Attach a CV to this application or upload your Master CV first.");
}
```

> **Кеш самоінвалідується:** Redis-ключ = `sha256(повний промпт)`. Змінився контекст CV → змінився хеш → новий LLM-виклик. Нічого вручну інвалідувати не треба.

#### Gap analysis — окремий випадок (свідомо phase 2)

`GapAnalysisService` матчить required-скіли проти **embeddings з `master_cv_skills`** (pgvector), не проти тексту. Щоб per-app CV годував і його, треба витягти скіли з прикріпленого CV, заембедити й матчити проти них (`application_cv_skills` + гілка в `computeAnalysis`). **Рекомендація:** цю ітерацію gap analysis лишаємо на Master CV і показуємо це в UI бейджем. Розширення — окрема задача (дублює пів CV-модуля).

### Крок 2.7 — Тести

- `ApplicationCvServiceTest`: upload зберігає + витягує текст; повторний upload замінює; delete прибирає; чужа заявка → 404.
- `CvContextResolverTest`: є CV → override; порожній текст → empty; немає → empty.
- (опц.) `ApplicationCvControllerIT`: multipart → 201; download → правильні content-type/disposition/байти.

---

# Фіча статуси: кастомні статуси (повна модель)

**Мета:** статуси — per-user записи в БД (add/rename/color/delete/reorder). Рух вільний (any→any). Спец-поведінка (`applied_at`, авто interview-prep) — через семантичний тип. State machine ретайриться.

## Частина A — модель даних

### Крок 3.1 — enum семантичних типів (не плутати зі старим `ApplicationStatus`)

`status/SystemStatusType.java`:

```java
/** Опційний семантичний маркер на статусі. Керує спец-поведінкою і статистикою. */
public enum SystemStatusType {
    SAVED, APPLIED, SCREENING, INTERVIEW, FINAL, OFFER, REJECTED, WITHDRAWN, GHOSTED
}
```

Кастомні статуси → `systemType = null`.

### Крок 3.2 — entity `status/StatusCategory.java`

```java
@Entity
@Table(name = "status_categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_status_categories_user_name", columnNames = {"user_id", "name"}))
@Getter @Setter
public class StatusCategory {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)             private String name;   // display: "Applied", "Take-home"
    @Column(nullable = false, length = 20) private String color; // hex "#3b82f6"
    @Column(nullable = false)             private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type")         private SystemStatusType systemType; // nullable → кастомний

    @Column(name = "is_terminal", nullable = false) private boolean terminal = false;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false)                    private Instant updatedAt;

    @PrePersist void onCreate() { var n = Instant.now(); createdAt = n; updatedAt = n; }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
```

### Крок 3.3 — Міграція `V19__create_status_categories.sql` (таблиця + сід для наявних юзерів)

```sql
CREATE TABLE status_categories (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL,
    name         VARCHAR(100) NOT NULL,
    color        VARCHAR(20)  NOT NULL,
    position     INTEGER      NOT NULL,
    system_type  VARCHAR(20),
    is_terminal  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_status_categories_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_status_categories_user_name UNIQUE (user_id, name),
    CONSTRAINT check_status_categories_system_type
        CHECK (system_type IS NULL OR system_type IN (
            'SAVED','APPLIED','SCREENING','INTERVIEW','FINAL',
            'OFFER','REJECTED','WITHDRAWN','GHOSTED'))
);

CREATE INDEX idx_status_categories_user_position
    ON status_categories (user_id, position);

-- Сід 9 дефолтних статусів для КОЖНОГО наявного юзера. PG18 має gen_random_uuid().
INSERT INTO status_categories
    (id, user_id, name, color, position, system_type, is_terminal, created_at, updated_at)
SELECT gen_random_uuid(), u.id, d.name, d.color, d.position, d.system_type, d.is_terminal, now(), now()
FROM users u
CROSS JOIN (VALUES
    ('Saved',     '#64748b', 0, 'SAVED',     FALSE),
    ('Applied',   '#3b82f6', 1, 'APPLIED',   FALSE),
    ('Screening', '#6366f1', 2, 'SCREENING', FALSE),
    ('Interview', '#a855f7', 3, 'INTERVIEW', FALSE),
    ('Final',     '#f59e0b', 4, 'FINAL',     FALSE),
    ('Offer',     '#22c55e', 5, 'OFFER',     TRUE),
    ('Rejected',  '#ef4444', 6, 'REJECTED',  TRUE),
    ('Withdrawn', '#9ca3af', 7, 'WITHDRAWN', TRUE),
    ('Ghosted',   '#9ca3af', 8, 'GHOSTED',   TRUE)
) AS d(name, color, position, system_type, is_terminal);
```

### Крок 3.4 — Міграція `V20__applications_status_id.sql`

```sql
ALTER TABLE applications ADD COLUMN status_id UUID;

-- мапимо старий enum → на щойно засіяний статус того ж юзера через system_type
UPDATE applications a
SET status_id = s.id
FROM status_categories s
WHERE s.user_id = a.user_id AND s.system_type = a.status;

ALTER TABLE applications ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE applications
    ADD CONSTRAINT fk_applications_status
    FOREIGN KEY (status_id) REFERENCES status_categories(id) ON DELETE RESTRICT;

ALTER TABLE applications DROP COLUMN status;  -- прибирає і CHECK

CREATE INDEX idx_applications_user_status_id
    ON applications (user_id, status_id) WHERE archived = FALSE;
```

### Крок 3.5 — Міграція `V21__status_history_labels.sql`

Історія має пережити перейменування/видалення статусу → текстові снапшоти, не FK.

```sql
ALTER TABLE application_status_history ADD COLUMN from_label VARCHAR(100);
ALTER TABLE application_status_history ADD COLUMN to_label   VARCHAR(100);

UPDATE application_status_history SET from_label = from_status WHERE from_status IS NOT NULL;
UPDATE application_status_history SET to_label   = to_status;

ALTER TABLE application_status_history ALTER COLUMN to_label SET NOT NULL;

ALTER TABLE application_status_history DROP COLUMN from_status;
ALTER TABLE application_status_history DROP COLUMN to_status;
```

`ApplicationStatusHistory.java` — замінити `@Enumerated ... fromStatus/toStatus` на `String fromLabel/toLabel`.

## Частина B — backend-логіка

### Крок 3.6 — Application entity

`application/Application.java`:

```java
// було: @Enumerated(EnumType.STRING) private ApplicationStatus status;
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "status_id", nullable = false)
private StatusCategory status;
```

### Крок 3.7 — Repository + StatusService

`status/StatusCategoryRepository.java`:

```java
public interface StatusCategoryRepository extends JpaRepository<StatusCategory, UUID> {
    List<StatusCategory> findByUserIdOrderByPositionAsc(UUID userId);
    Optional<StatusCategory> findByIdAndUserId(UUID id, UUID userId);
    Optional<StatusCategory> findByUserIdAndSystemType(UUID userId, SystemStatusType type);
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);
    long countByUserId(UUID userId);
}
```

`status/StatusService.java`:

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusService {

    private final StatusCategoryRepository repository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public List<StatusResponse> list(UUID userId) {
        return repository.findByUserIdOrderByPositionAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public StatusResponse create(UUID userId, CreateStatusRequest req) {
        if (repository.existsByUserIdAndNameIgnoreCase(userId, req.getName().trim()))
            throw new ConflictException("Status '" + req.getName() + "' already exists");
        StatusCategory s = new StatusCategory();
        s.setUser(userRepository.getReferenceById(userId));
        s.setName(req.getName().trim());
        s.setColor(req.getColor());
        s.setSystemType(null); // користувацькі статуси завжди без семантики
        s.setTerminal(req.isTerminal());
        s.setPosition((int) repository.countByUserId(userId)); // у кінець
        return toResponse(repository.save(s));
    }

    @Transactional
    public StatusResponse update(UUID userId, UUID id, UpdateStatusCategoryRequest req) {
        StatusCategory s = fetchOwned(userId, id);
        if (req.getName() != null)  s.setName(req.getName().trim());
        if (req.getColor() != null) s.setColor(req.getColor());
        if (req.getTerminal() != null) s.setTerminal(req.getTerminal());
        return toResponse(repository.save(s));
    }

    /** Видалення з опційним переносом заявок у інший статус. */
    @Transactional
    public void delete(UUID userId, UUID id, UUID reassignToId) {
        StatusCategory s = fetchOwned(userId, id);
        if (repository.countByUserId(userId) <= 1)
            throw new BusinessRuleException("Cannot delete the last status");
        long inUse = applicationRepository.countByStatusId(id);
        if (inUse > 0) {
            if (reassignToId == null)
                throw new BusinessRuleException(
                    "This status has " + inUse + " applications. Provide a status to move them to.");
            StatusCategory target = fetchOwned(userId, reassignToId);
            applicationRepository.reassignStatus(id, target.getId());
        }
        repository.delete(s);
    }

    @Transactional
    public List<StatusResponse> reorder(UUID userId, List<UUID> orderedIds) {
        List<StatusCategory> owned = repository.findByUserIdOrderByPositionAsc(userId);
        Map<UUID, StatusCategory> byId = owned.stream().collect(Collectors.toMap(StatusCategory::getId, s -> s));
        int pos = 0;
        for (UUID sid : orderedIds) {
            StatusCategory s = byId.get(sid);
            if (s == null) throw new BusinessRuleException("Unknown status: " + sid);
            s.setPosition(pos++);
        }
        repository.saveAll(owned);
        return list(userId);
    }

    /** Сід дефолтів для нового юзера (виклик з AuthService.register). */
    @Transactional
    public void seedDefaults(User user) {
        record Def(String name, String color, SystemStatusType type, boolean terminal) {}
        List<Def> defs = List.of(
            new Def("Saved","#64748b",SystemStatusType.SAVED,false),
            new Def("Applied","#3b82f6",SystemStatusType.APPLIED,false),
            new Def("Screening","#6366f1",SystemStatusType.SCREENING,false),
            new Def("Interview","#a855f7",SystemStatusType.INTERVIEW,false),
            new Def("Final","#f59e0b",SystemStatusType.FINAL,false),
            new Def("Offer","#22c55e",SystemStatusType.OFFER,true),
            new Def("Rejected","#ef4444",SystemStatusType.REJECTED,true),
            new Def("Withdrawn","#9ca3af",SystemStatusType.WITHDRAWN,true),
            new Def("Ghosted","#9ca3af",SystemStatusType.GHOSTED,true));
        int pos = 0;
        for (Def d : defs) {
            StatusCategory s = new StatusCategory();
            s.setUser(user);
            s.setName(d.name()); s.setColor(d.color());
            s.setSystemType(d.type()); s.setTerminal(d.terminal());
            s.setPosition(pos++);
            repository.save(s);
        }
    }

    private StatusCategory fetchOwned(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found: " + id));
    }

    private StatusResponse toResponse(StatusCategory s) {
        StatusResponse r = new StatusResponse();
        r.setId(s.getId()); r.setName(s.getName()); r.setColor(s.getColor());
        r.setPosition(s.getPosition()); r.setSystemType(s.getSystemType()); r.setTerminal(s.isTerminal());
        return r;
    }
}
```

`ApplicationRepository` — додати:

```java
long countByStatusId(UUID statusId);

@Modifying
@Query("update Application a set a.status.id = :target where a.status.id = :from")
void reassignStatus(@Param("from") UUID from, @Param("target") UUID target);
```

### Крок 3.8 — Сід на реєстрації

`auth/AuthService.register(...)` — після `userRepository.save(user)`:

```java
User saved = userRepository.save(user);
statusService.seedDefaults(saved);   // інжектнути StatusService
return jwtService.generateTokens(saved);
```

### Крок 3.9 — StatusController + DTO

`status/StatusController.java`:

```java
@RestController
@RequestMapping("/api/v1/statuses")
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    public List<StatusResponse> list(@AuthenticationPrincipal CustomUserDetails p) {
        return statusService.list(p.user().getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StatusResponse create(@AuthenticationPrincipal CustomUserDetails p,
                                 @Valid @RequestBody CreateStatusRequest req) {
        return statusService.create(p.user().getId(), req);
    }

    @PatchMapping("/{id}")
    public StatusResponse update(@AuthenticationPrincipal CustomUserDetails p,
                                 @PathVariable UUID id,
                                 @Valid @RequestBody UpdateStatusCategoryRequest req) {
        return statusService.update(p.user().getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CustomUserDetails p,
                       @PathVariable UUID id,
                       @RequestParam(required = false) UUID reassignTo) {
        statusService.delete(p.user().getId(), id, reassignTo);
    }

    @PatchMapping("/reorder")
    public List<StatusResponse> reorder(@AuthenticationPrincipal CustomUserDetails p,
                                        @Valid @RequestBody ReorderStatusesRequest req) {
        return statusService.reorder(p.user().getId(), req.getOrderedIds());
    }
}
```

DTO (усе `@Getter @Setter`, крім реквестів з валідацією):
- `StatusResponse { UUID id; String name; String color; int position; SystemStatusType systemType; boolean terminal; }`
- `CreateStatusRequest { @NotBlank String name; @NotBlank String color; boolean terminal; }`
- `UpdateStatusCategoryRequest { String name; String color; Boolean terminal; }`
- `ReorderStatusesRequest { @NotEmpty List<UUID> orderedIds; }`

**Фронт-контракт:** `GET/POST/PATCH/DELETE /api/v1/statuses`, `PATCH /api/v1/statuses/reorder {orderedIds:[...]}`. DELETE приймає `?reassignTo={id}`.

### Крок 3.10 — updateStatus без state machine

`ApplicationService.updateStatus(...)`:

```java
@Transactional
public ApplicationResponse updateStatus(UUID userId, UUID id, UpdateStatusRequest r) {
    Application app = fetchOwned(userId, id);
    StatusCategory target = statusCategoryRepository.findByIdAndUserId(r.getStatusId(), userId)
            .orElseThrow(() -> new ResourceNotFoundException("Status not found: " + r.getStatusId()));

    StatusCategory from = app.getStatus();
    app.setStatus(target);

    if (target.getSystemType() == SystemStatusType.APPLIED && app.getAppliedAt() == null) {
        app.setAppliedAt(Instant.now());
    }

    Application saved = applicationRepository.save(app);
    writeHistory(saved, from, target, r.getNote());

    if (target.getSystemType() == SystemStatusType.INTERVIEW) {
        interviewPrepService.createIfNotExists(saved.getId());
    }
    return applicationMapper.toResponse(saved);
}

private void writeHistory(Application app, StatusCategory from, StatusCategory to, String note) {
    ApplicationStatusHistory h = new ApplicationStatusHistory();
    h.setApplication(app);
    h.setFromLabel(from != null ? from.getName() : null);
    h.setToLabel(to.getName());
    h.setNote(note);
    statusHistoryRepository.save(h);
}
```

- `UpdateStatusRequest` — `@NotNull UUID statusId` замість enum.
- `create(...)` — приймати опційний `statusId`; якщо null → дефолтний статус юзера (`findByUserIdAndSystemType(userId, SAVED)` або перший за position). Замінити `app.setStatus(ApplicationStatus.SAVED)`.
- **URL-parsing** (`createFromUrl`): де ставилось `ApplicationStatus.SAVED` — резолвити дефолтний SAVED-статус.
- **Видалити** `ApplicationStateMachine.java` + `ApplicationStateMachineTest.java` (81 тест). Talking point: «спочатку строгий FSM, але кастомні колонки роблять жорсткі переходи некерованими → вільний Kanban зі снапшот-історією».

### Крок 3.11 — DTO/Mapper/Filters/Parsing

- `ApplicationResponse` / `ApplicationSummaryResponse`: замість `ApplicationStatus status` → вкладений `StatusSummary status { UUID id; String name; String color; SystemStatusType systemType; }`. `ApplicationMapper` (MapStruct) — мапінг `StatusCategory → StatusSummary`. **Це фронт-контракт статусу заявки.**
- `ApplicationFilters`: `List<ApplicationStatus> statuses` → `List<UUID> statusIds`. `ApplicationSpecifications.byStatus` → `byStatusIds`: `root.get("status").get("id").in(ids)`.
- Грепнути весь бекенд на `ApplicationStatus` (старий enum) і замінити всі використання; сам enum видалити після зачистки (лишається `SystemStatusType`).

### Крок 3.12 — Тести (backend)

- `StatusServiceTest`: create (dup → 409), delete останнього → 422, delete зайнятого без reassign → 422, delete з reassign переносить, reorder виставляє position, seedDefaults створює 9.
- `ApplicationServiceUpdateStatusTest` (закриває борг 6 з KNOWN_ISSUES): INTERVIEW-тип тригерить interview-prep; APPLIED-тип виставляє applied_at; будь-який перехід дозволено; історія пише мітки.
- Оновити `ApplicationControllerIT` під `statusId`.
- Видалити `ApplicationStateMachineTest`.

---

## Ризики й на що звернути увагу

1. **Порядок міграцій V19→V20→V21 незворотний** — тестувати спершу локально (свіжа база), звірити що наявні заявки коректно змапились через `system_type`. На проді Neon backup перед деплоєм.
2. **`bytea` і Neon storage** (CV): зараз storage 8%. Обмежити розмір файлу в `CvFileValidation` (напр. 5 МБ).
3. **`byte[]` LAZY у JPA** ліниться надійно лише з інструментацією; тому байти беремо тільки на `/download`, метадані — через projection. Уже враховано.
4. **Зачистка старого enum:** грепнути `ApplicationStatus` по бекенду до нуля використань перед мержем (легко пропустити parsing/DTO/filters).
5. **Кеш AI** (CV): нічого не інвалідувати вручну — content-hashed ключ зробить це сам.
6. **Після всіх фіч** повернутись до `KNOWN_ISSUES.md`: закриються 5.6, 5.7 і ретайр FSM.

## Підсумковий backend-чеклист

**Архів:** контролер (delete=hard, +archive/unarchive) · `ApplicationFilters.archived` + spec · тести.

**Per-app CV:** `V18` · `ApplicationCv` + repo(+projection) · `ApplicationCvService` · контролер(upload/get/download/delete) · `ApplicationCvMetaResponse` · `CvContextResolver` · інжект у 3 генератори · тести. (gap analysis — окремо).

**Статуси:** `SystemStatusType` · `StatusCategory`+repo · `V19/V20/V21` · `StatusService`+seed · `StatusController`+4 DTO · Application entity/mapper/filters/parsing · `updateStatus` без FSM · видалити FSM · тести.
