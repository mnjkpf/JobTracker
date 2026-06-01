package com.jobtracker.backendJobTracker.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Unit-тести для {@link ApplicationSpecifications} через мок CriteriaBuilder (без БД).
 * <p>
 * Ключовий контракт: null/empty фільтр -> {@code cb.conjunction()} ("WHERE TRUE", no-op),
 * не-null фільтр -> реальна умова (no-op гілка пропускається, викликається відповідний builder).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class ApplicationSpecificationsTest {

    @Mock private Root<Application> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path path;
    @Mock private Predicate conjunction;
    @Mock private Predicate predicate;

    @BeforeEach
    void setUp() {
        when(cb.conjunction()).thenReturn(conjunction);
        when(root.get(anyString())).thenReturn(path);
        when(path.get(anyString())).thenReturn(path);
        when(cb.lower(any(Expression.class))).thenReturn(path);
        // LIKE має повертати не-null, щоб searchQuery склав cb.or(predicate, predicate).
        when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
    }

    private Predicate toPredicate(org.springframework.data.jpa.domain.Specification<Application> spec) {
        return spec.toPredicate(root, query, cb);
    }

    // ── Mandatory filters (без null-гілки) ──────────────────────────────

    @Test
    @DisplayName("byUser завжди генерує рівність по user.id (без conjunction)")
    void byUser() {
        toPredicate(ApplicationSpecifications.byUser(UUID.randomUUID()));
        verify(cb, never()).conjunction();
        verify(cb).equal(any(Expression.class), any(Object.class));
    }

    @Test
    @DisplayName("notArchived завжди генерує isFalse(archived)")
    void notArchived() {
        toPredicate(ApplicationSpecifications.notArchived());
        verify(cb, never()).conjunction();
        verify(cb).isFalse(any(Expression.class));
    }

    // ── byStatus (multi-value) ──────────────────────────────────────────

    @Test
    @DisplayName("byStatus(null) і byStatus(empty) -> conjunction")
    void byStatus_nullOrEmpty() {
        assertThat(toPredicate(ApplicationSpecifications.byStatus(null))).isSameAs(conjunction);
        assertThat(toPredicate(ApplicationSpecifications.byStatus(List.of()))).isSameAs(conjunction);
    }

    @Test
    @DisplayName("byStatus(non-empty) -> IN, не conjunction")
    void byStatus_values() {
        toPredicate(ApplicationSpecifications.byStatus(List.of(ApplicationStatus.APPLIED, ApplicationStatus.SAVED)));
        verify(cb, never()).conjunction();
        verify(path).in(any(java.util.Collection.class));
    }

    // ── Single-value equality filters ───────────────────────────────────

    @Test
    @DisplayName("byContractType: null -> conjunction, value -> equal")
    void byContractType() {
        assertThat(toPredicate(ApplicationSpecifications.byContractType(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.byContractType(ContractType.B2B));
        verify(cb).equal(any(Expression.class), any(Object.class));
    }

    @Test
    @DisplayName("byWorkMode: null -> conjunction, value -> equal")
    void byWorkMode() {
        assertThat(toPredicate(ApplicationSpecifications.byWorkMode(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.byWorkMode(WorkMode.REMOTE));
        verify(cb).equal(any(Expression.class), any(Object.class));
    }

    @Test
    @DisplayName("bySourceBoard: null -> conjunction, value -> equal")
    void bySourceBoard() {
        assertThat(toPredicate(ApplicationSpecifications.bySourceBoard(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.bySourceBoard(SourceBoard.LINKEDIN));
        verify(cb).equal(any(Expression.class), any(Object.class));
    }

    @Test
    @DisplayName("byCompany: null -> conjunction, value -> equal по company.id")
    void byCompany() {
        assertThat(toPredicate(ApplicationSpecifications.byCompany(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.byCompany(UUID.randomUUID()));
        verify(cb).equal(any(Expression.class), any(Object.class));
    }

    // ── bySeniority (multi-value) ───────────────────────────────────────

    @Test
    @DisplayName("bySeniority: empty -> conjunction, values -> IN")
    void bySeniority() {
        assertThat(toPredicate(ApplicationSpecifications.bySeniority(List.of()))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.bySeniority(List.of(Seniority.JUNIOR, Seniority.JUNIOR_PLUS)));
        verify(path).in(any(java.util.Collection.class));
    }

    // ── searchQuery ─────────────────────────────────────────────────────

    @Test
    @DisplayName("searchQuery: null/blank -> conjunction")
    void searchQuery_blank() {
        assertThat(toPredicate(ApplicationSpecifications.searchQuery(null))).isSameAs(conjunction);
        assertThat(toPredicate(ApplicationSpecifications.searchQuery("   "))).isSameAs(conjunction);
    }

    @Test
    @DisplayName("searchQuery: текст -> LIKE по name та description (OR)")
    void searchQuery_text() {
        toPredicate(ApplicationSpecifications.searchQuery("java"));
        verify(cb, never()).conjunction();
        verify(cb, times(2)).like(any(Expression.class), anyString());
        verify(cb).or(any(Predicate.class), any(Predicate.class));
    }

    // ── Date range ──────────────────────────────────────────────────────

    @Test
    @DisplayName("appliedAfter: null -> conjunction, value -> >=")
    void appliedAfter() {
        assertThat(toPredicate(ApplicationSpecifications.appliedAfter(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.appliedAfter(Instant.now()));
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Instant.class));
    }

    @Test
    @DisplayName("appliedBefore: null -> conjunction, value -> <=")
    void appliedBefore() {
        assertThat(toPredicate(ApplicationSpecifications.appliedBefore(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.appliedBefore(Instant.now()));
        verify(cb).lessThanOrEqualTo(any(Expression.class), any(Instant.class));
    }

    // ── Salary ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("minSalaryAtLeast: null -> conjunction, value -> >= по salaryMax")
    void minSalaryAtLeast() {
        assertThat(toPredicate(ApplicationSpecifications.minSalaryAtLeast(null))).isSameAs(conjunction);

        toPredicate(ApplicationSpecifications.minSalaryAtLeast(8000));
        verify(cb).greaterThanOrEqualTo(any(Expression.class), any(Integer.class));
    }
}
