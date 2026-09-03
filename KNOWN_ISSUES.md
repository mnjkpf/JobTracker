# JobTracker — реєстр відомих проблем і технічного боргу

> Зібрано з аудиту репозиторію (backend + frontend + git) станом на 2026-08-31.
> **Проект зараз працює** — це backlog, не пожежа. Повертаємось сюди після реалізації нових ідей.
>
> Легенда пріоритетів:
> - 🔴 **P0** — треба зробити до наступного `git add`/деплою (безпека, git-гігієна).
> - 🟠 **P1** — інфраструктура: коштує грошей / ламає prod при навантаженні.
> - 🟡 **P2** — функціональні недоробки, не блокують.
> - 🔵 **P3** — портфоліо-артефакти й документація.
> - ⚪ **P4** — косметика.

---

## 0. Контекст

Робоча копія на гілці `develope` (через «e»). `origin/main` = `develope` + 2 merge-коміти + README.
Railway слухає `develope`, Vercel — `main`. Проект живий на:
- Frontend: https://job-tracker-six-flame.vercel.app
- Backend: https://jobtracker-production-2e75.up.railway.app

---

## 1. 🔴 Безпека (P0)

### 1.1 Секрети у корені репозиторію
У `D:\JobTracker\` лежать: `deploy-secrets.txt`, `jwt.txt`, `keys.txt`, `upstash.txt`.
- В git-історії їх **немає** (перевірено `git log --all --diff-filter=A` по всіх гілках) — тобто ще не закомічені.
- **АЛЕ root `.gitignore` не існує.** Один необережний `git add .` — і це четвертий інцидент з ключами.
- Fix: створити root `.gitignore` (додати `*.txt` секрети, `.env`, тощо) **або** винести ці файли за межі репо. Перевірити, що всі ключі в них уже й так відкликані/актуальні лише в Railway Variables.

### 1.2 Немає root `.gitignore`
Є тільки `backendJobTracker/.gitignore` і `frontend/.gitignore`. Кореневий рівень не захищений взагалі.

---

## 2. 🔴 Git-гігієна (P0)

### 2.1 Робоче дерево «зламане» через CRLF/LF
`git status` показує **405 файлів як modified**, `git diff --stat` = **31 505 вставок / 31 505 видалень** — це не реальні зміни, а перенос рядків: файли на диску CRLF, в індексі LF.
- Поки не полагоджено, `git status` непридатний, а будь-який коміт роздується на ~31k рядків.
- Fix: узгодити `.gitattributes` (додати `* text=auto` або явні правила), потім `git add --renormalize .` в окремому «normalize line endings» коміті. Зробити це **до** будь-яких нових комітів, щоб реальні зміни було видно в diff.

### 2.2 Локальний `main` відстає від `origin/main` на 109 комітів
Локальний `main` = «starter files», `origin/main` вже містить весь проект (frontend + бекенд-фікси + README).
- Fix: `git checkout main && git pull` (або взагалі не тримати локальний main).

### 2.3 Неузгодженість гілок Railway/Vercel *(з контексту)*
Railway → `develope`, Vercel → `main`. Варто привести обидва до `main`.

### 2.4 README на `origin/main` без markdown-розмітки
README **вже закомічений** на `origin/main`, але без жодного `#`, списку чи форматування — на GitHub рендериться суцільним абзацом. (У робочій копії на `develope` його не видно, тому легко забути.)
- Fix: переписати з нормальним markdown (заголовки, списки, code-блоки, бейджі).

---

## 3. 🟠 Інфраструктура (P1) *(переважно з контексту, досі актуально)*

### 3.1 HikariCP pool fix НЕ застосований
`application-prod.properties` досі має `spring.datasource.hikari.minimum-idle=1` → Neon не може scale-to-zero → compute горить 24/7 при нульовому навантаженні. Це жива причина спаленої квоти.
- Fix (з контексту, не застосований):
  ```properties
  spring.datasource.hikari.minimum-idle=0
  spring.datasource.hikari.idle-timeout=30000
  spring.datasource.hikari.max-lifetime=300000
  spring.datasource.hikari.keepalive-time=0
  ```

### 3.2 DB/Redis health indicators увімкнені
`application.properties`: `management.health.redis.enabled=true`, db indicator не вимкнено → Railway healthcheck на `/actuator/health` періодично будить базу.
- Fix: `management.health.db.enabled=false`, `management.health.redis.enabled=false` у prod.

### 3.3 Neon / Railway квоти *(з контексту)*
- Neon compute вичерпано (110/100 CU-hours). Створити новий Neon-проект або дочекатись reset.
- Railway trial вичерпано ($6.49/$5.00). Варіанти: Hobby $5/міс, або RAM 512 MB (~$2.90), або Fly.io (free scale-to-zero).
- Перевірити й видалити зайвий Railway-проект `courageous-victory` ($0.70, походження невідоме).

---

## 4. 🟠 Заявлено, але не реалізовано (P1 — dead config / README overclaim)

> Ці речі є в залежностях/конфігах/README, але в коді їх поведінки немає. Небезпечно для співбесіди — інтерв'юер може спитати «покажи де».

### 4.1 Rate limiting (Bucket4j) — не використовується ніде
`bucket4j` у `pom.xml`, є секція `jobtracker.rate-limit.*` у properties і в `JobTrackerProperties`, але **жодного бакета/фільтра в коді немає**. README обіцяє «distributed rate limiting via Redis».
- Рішення: або реалізувати (фільтр на `/auth/**`), або прибрати з README + залежностей.

### 4.2 Circuit breakers (Resilience4j) — не використовуються ніде
`resilience4j` у `pom.xml`, але жодного `@CircuitBreaker`/`@Retry` навколо LLM-викликів. README обіцяє «Circuit breakers around LLM calls».
- Рішення: або обгорнути `AiService.complete` / `EmbeddingService`, або прибрати claim.

### 4.3 Email verification — не enforced + фантомна property
- `jobtracker.auth.email-verification-required` згадана в контексті, але **в коді її не існує**.
- `register()` одразу видає токени; при `login()` `isEmailVerified` не перевіряється — verification-флоу є (endpoints + сервіси), але нічого не блокує.
- Рішення: або enforce (перевірка при логіні за прапорцем), або задокументувати як свідоме MVP-рішення.

### 4.4 `jobtracker.ai.cache-ttl` — мертва property
Оголошена й валідується в `JobTrackerProperties`, але `AiResponseCache` хардкодить `Duration.ofDays(7)`, `EmbeddingCache` — 30 днів. Property ні на що не впливає.
- Fix: інжектити TTL з property, або прибрати.

### 4.5 Дубль-клас `interview/rag/dto/InterviewPrepResponse`
Існує другий `InterviewPrepResponse` у пакеті `interview.rag.dto`, який **ніде не імпортується** (усі використовують `interview.dto.InterviewPrepResponse`).
- Fix: видалити мертвий клас.

---

## 5. 🟡 Функціональні баги / недоробки (P2)

### 5.1 JustJoinIt і NoFluffJobs парсери — фактично копії, селектори не спрацюють
`JustJoinItParser` і `NoFluffJobsParser` мають **однакові плейсхолдер-селектори** (`h1.posting-title`, `a.company-name`, `span.location`, `div.job-description`), яких на реальних сайтах немає → обидва завжди повертають empty → усе падає в LLM fallback. Тільки `PracujParser` має справжні `data-test` селектори.
- Наслідок: детерміністичний парсинг для 2 з 3 бордів не працює (але LLM-фолбек рятує, тому «працює»).
- Fix: або дописати реальні селектори, або чесно лишити тільки Pracuj + LLM і задокументувати.

### 5.2 CV PATCH endpoints null-safe overwrite *(з контексту)*
Не можна очистити поле назад у null (напр. зняти `endDate` з experience). `updateDetails` та CV-патчі роблять `if (r.getX() != null) set(...)`.
- Fix: розрізняти «поле відсутнє» vs «поле = null» (напр. JSON Merge Patch / Optional-обгортки).

### 5.3 Kanban search шукає тільки name + description *(з контексту)*
`ApplicationSpecifications.searchQuery` не включає companyName. UI-плейсхолдер це вже чесно каже.
- Fix: додати join на company.name у пошук.

### 5.4 N+1 при мапінгу companyName у списку заявок *(з контексту)*
LAZY company в read-only транзакції. Для демо непомітно.
- Fix: `@EntityGraph` на list-запиті.

### 5.5 Gap threshold 0.75 занадто високий *(з контексту)*
«rest apis» не матчиться з «rest api». Емпіричне значення з ранніх ітерацій.
- Fix: потюнити `jobtracker.similarity.gap-threshold`.

### 5.6 Frontend `VALID_TRANSITIONS` не має self-loop INTERVIEW→INTERVIEW
Бекенд `ApplicationStateMachine` дозволяє `INTERVIEW → INTERVIEW` (наступні раунди), а фронтовий `VALID_TRANSITIONS.INTERVIEW = ['FINAL','REJECTED','WITHDRAWN','GHOSTED']` — ні. Тобто UI не дає лишитись на INTERVIEW для нового раунду, хоча API б дозволив.
- Fix: додати `'INTERVIEW'` у список переходів INTERVIEW на фронті (дзеркалити бекенд).

### 5.7 WITHDRAWN / GHOSTED приховані в Kanban *(з контексту, MVP-рішення)*
`KANBAN_COLUMNS` не містить цих статусів. Свідомо, але варто задокументувати / додати окремий вид.

### 5.8 Top Technologies на Statistics пропущено *(з контексту)*
Немає bulk-endpoint для ApplicationSkill; агрегувати дорого.
- Fix: окремий агрегаційний endpoint.

### 5.9 Statistics рахуються client-side з `?size=500`
`useStatistics` тягне до 500 заявок і рахує в браузері. Для демо ок, для масштабу — ні.
- Fix: серверний statistics-endpoint.

---

## 6. 🟡 Тестове покриття (P2)

Overall ~52% (низький через ненаписані controller integration tests), 96.3% на `interview.rag`.
Написано: unit-тести service-шару, `InterviewNoteSimilaritySearchIT` (Testcontainers + pgvector), `GlobalExceptionHandlerIT`, `ApplicationControllerIT`, `AuthControllerIT`, `CompanyControllerIT`, `ApplicationStatusHistoryIT`.

Не написано *(свідомо відкладено, з контексту)*:
- RAG End-to-End IT через HTTP (потребує non-transactional harness — `AbstractIntegrationTest` має `@Transactional`, async afterCommit hook не спрацьовує).
- `InterviewPrepControllerIT`, `InterviewNoteControllerIT` (MockMvc).
- `InterviewNoteRepositoryTest` (`@DataJpaTest`), `InterviewPrepMapperTest`, `ApplicationServiceUpdateStatusTest`.

Jacoco налаштований (0.8.12) **без check-gate**.

---

## 7. 🔵 Портфоліо-артефакти (P3)

- [ ] README у корені — переписати з нормальним markdown (див. 2.4).
- [ ] `backendJobTracker/README.md` застарілий: «Week 1 of 12», React 18 (реально 19), Postgres 16 (реально 18), LaTeX/PDF export (реально DOCX), податковий калькулятор B2B/UoP/UZ (не існує), посилання на неіснуючий `docs/`.
- [ ] Каталогу `docs/` немає взагалі — ні ADR, ні screenshots.
- [ ] 4 ADR у `docs/adr/`: monolith, pgvector, content-hashed cache, RAG architecture.
- [ ] Screenshots у `docs/screenshots/`.
- [ ] Screen recording demo 90–120 сек (Kanban → створення → Gap Analysis → Cover Letter → Tailored CV + DOCX → **Interview Prep з RAG** → Statistics).
- [ ] LinkedIn post з відео; pin repo на GitHub-профілі.
- [ ] CI немає: `.github/` містить лише хуки від `modernize/java-upgrade`. Додати GitHub Actions (build + test).

---

## 8. ⚪ Косметика (P4)

- Опечатки в іменах міграцій: `V3__create_veriricationtoken_table.sql`, `V9__create_conver_letter_table.sql`. Перейменовувати вже застосовані міграції **не можна** (Flyway checksum) — лишити як є, врахувати на майбутнє.
- `application-test.properties`: `jobtracker.auth.jwt-secret` = сирий рядок з дефісами, не валідний Base64. Dev/prod декодують secret через `Decoders.BASE64.decode`. Потенційно тести, що реально підписують/парсять JWT, могли б падати — **але проект збирається, тож або цей шлях у тестах не задіяний, або є override.** Перевірити при нагоді (низька впевненість, не діагностовано).
- `salaryMin > salaryMax` з парсера мовчки відкидається (є коментар у коді) — ок, просто знати.

---

## Зведення по пріоритетах

| Пріоритет | Пунктів | Суть |
|---|---|---|
| 🔴 P0 | 1.1, 1.2, 2.1, 2.2 | Секрети + CRLF + локальний main — зробити до наступного коміту |
| 🟠 P1 | 3.1–3.3, 4.1–4.5 | Neon/Railway гроші + dead config / README overclaim |
| 🟡 P2 | 5.1–5.9, 6 | Функціональні недоробки + тести |
| 🔵 P3 | 7 | Портфоліо: README, docs, ADR, demo, CI |
| ⚪ P4 | 8 | Косметика |
