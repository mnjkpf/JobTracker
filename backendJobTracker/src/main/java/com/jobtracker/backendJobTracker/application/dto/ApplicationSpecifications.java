package com.jobtracker.backendJobTracker.application.dto;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
 
import org.springframework.data.jpa.domain.Specification;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
 
import jakarta.persistence.criteria.Predicate;
 


/**
 * Динамічні WHERE-умови для list endpoint Application.
 * <p>
 * Кожен static метод повертає {@link Specification} — це builder одного шматка WHERE.
 * У service ці шматки композитяться через {@code Specification.where(a).and(b).and(c)}.
 * <p>
 * Null/empty фільтр → {@code cb.conjunction()} (буквально "WHERE TRUE") →
 * нічого не додається до згенерованого SQL. Завдяки цьому service не пише
 * if'и навколо кожного фільтра — null pass-through "вмикає"/"вимикає" умову автоматично.
 * <p>
 * Pattern: utility class (private constructor + static методи) — як HashUtil.
 * Жодного state, жодних залежностей — pure functions. Тестується unit-тестами
 * без Spring контексту і БД.
 */
public final class ApplicationSpecifications {
 
    private ApplicationSpecifications() {
        // Utility class — instances не потрібні.
    }
 
    // ═══════════════════════════════════════════════════════════════
    // MANDATORY FILTERS (завжди передаються у service)
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * Tenant isolation — фундамент multi-user системи.
     * Без цього фільтра запит поверне applications інших юзерів. Має бути ПЕРШИМ у chain.
     * <p>
     * {@code root.get("user").get("id")} — це навігація по object graph:
     * "візьми user поле Application, потім візьми id поле user".
     * <p>
     * JPA згенерує: {@code WHERE user_id = ?} БЕЗ реального JOIN на users —
     * бо ми звертаємось лише до id (який вже зберігається в applications.user_id як FK).
     * Це Hibernate optimization для @ManyToOne з access лише до id.
     */
    public static Specification<Application> byUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }
 
    /**
     * Soft delete — за замовчуванням list endpoint приховує archived заявки.
     * Архівні видно тільки через окремий explicit endpoint (якщо взагалі додамо).
     */
    public static Specification<Application> notArchived() {
        return (root, query, cb) -> cb.isFalse(root.get("archived"));
    }
 
    // ═══════════════════════════════════════════════════════════════
    // OPTIONAL FILTERS (приходять з UI як query params)
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * Multi-value: користувач може вибрати кілька статусів одночасно
     * ("показати всі APPLIED і SCREENING").
     * <p>
     * SQL: {@code WHERE status IN (?, ?, ?)} — efficient, використовує index якщо є.
     */
    public static Specification<Application> byStatus(Collection<ApplicationStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();  // "WHERE TRUE" — no-op
            }
            return root.get("status").in(statuses);
        };
    }
 
    /**
     * Single-value: контракт зазвичай фільтрується по одному значенню
     * (хочу бачити тільки B2B вакансії).
     */
    public static Specification<Application> byContractType(ContractType contractType) {
        return (root, query, cb) -> {
            if (contractType == null) return cb.conjunction();
            return cb.equal(root.get("contractType"), contractType);
        };
    }
 
    /**
     * Multi-value: junior може хотіти бачити одночасно JUNIOR і JUNIOR_PLUS.
     */
    public static Specification<Application> bySeniority(Collection<Seniority> seniorities) {
        return (root, query, cb) -> {
            if (seniorities == null || seniorities.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("seniority").in(seniorities);
        };
    }
 
    public static Specification<Application> byWorkMode(WorkMode workMode) {
        return (root, query, cb) -> {
            if (workMode == null) return cb.conjunction();
            return cb.equal(root.get("workMode"), workMode);
        };
    }
 
    public static Specification<Application> bySourceBoard(SourceBoard sourceBoard) {
        return (root, query, cb) -> {
            if (sourceBoard == null) return cb.conjunction();
            return cb.equal(root.get("sourceBoard"), sourceBoard);
        };
    }
 
    /**
     * Фільтр по компанії — через FK company_id.
     * Той самий optimization як з user.id: JPA не робить реальний JOIN на companies,
     * просто {@code WHERE company_id = ?} напряму.
     */
    public static Specification<Application> byCompany(UUID companyId) {
        return (root, query, cb) -> {
            if (companyId == null) return cb.conjunction();
            return cb.equal(root.get("company").get("id"), companyId);
        };
    }
 
    /**
     * Текстовий пошук у name + description (case-insensitive через cb.lower).
     * <p>
     * SQL: {@code WHERE LOWER(name) LIKE '%java%' OR LOWER(description) LIKE '%java%'}
     * <p>
     * ВАЖЛИВО про продуктивність: leading wildcard ('%q%') НЕ використовує B-tree index.
     * Це full table scan. Для MVP scale (десятки-сотні заявок одного юзера) — OK.
     * При тисячах записів треба буде переходити на Postgres tsvector + GIN index
     * (full-text search). Це робота на майбутнє — TODO у W9 polish.
     * <p>
     * NULL поля: якщо description = null, {@code NULL LIKE 'pattern'} = NULL = false у SQL.
     * Тобто рядок не зматчиться, але і не зламає запит — бажана поведінка.
     */
    public static Specification<Application> searchQuery(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
 
            String pattern = "%" + q.toLowerCase() + "%";
            Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
            Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
 
            return cb.or(nameMatch, descMatch);
        };
    }
 
    // ═══════════════════════════════════════════════════════════════
    // DATE RANGE FILTERS
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * "Покажи заявки, на які я подався після X дати".
     * <p>
     * appliedAt може бути null (для status=SAVED, коли юзер ще не подавався).
     * Такі рядки автоматично виключаються — {@code NULL >= timestamp} у SQL = NULL = false.
     */
    public static Specification<Application> appliedAfter(Instant instant) {
        return (root, query, cb) -> {
            if (instant == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("appliedAt"), instant);
        };
    }
 
    public static Specification<Application> appliedBefore(Instant instant) {
        return (root, query, cb) -> {
            if (instant == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("appliedAt"), instant);
        };
    }
 
    // ═══════════════════════════════════════════════════════════════
    // SALARY RANGE (для майбутніх "min salary >= 8000" фільтрів)
    // ═══════════════════════════════════════════════════════════════
 
    /**
     * Заявки з salary_max >= X. Корисно для "не хочу нижче 8000 PLN".
     * Заявки де salary не вказана (null) виключаються — це consistency
     * (якщо немає salary даних, ми не можемо стверджувати що вона "достатньо висока").
     */
    public static Specification<Application> minSalaryAtLeast(Integer minAmount) {
        return (root, query, cb) -> {
            if (minAmount == null) return cb.conjunction();
            return cb.greaterThanOrEqualTo(root.get("salaryMax"), minAmount);
        };
    }
}

