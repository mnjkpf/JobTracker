package com.jobtracker.backendJobTracker.cv.tailored.export;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredEducationResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredExperienceResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredLanguageResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredProjectResponse;
import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredSkillResponse;

import lombok.RequiredArgsConstructor;

/**
 * Збирає DOCX з TailoredCvResponse. Використовує {@link DocxSectionBuilder}
 * для всіх low-level POI операцій.
 * <p>
 * <b>Структура CV (порядок секцій):</b>
 * <ol>
 *  <li>Hero block — name + headline + contact</li>
 *  <li>Summary</li>
 *  <li>Experience</li>
 *  <li>Skills</li>
 *  <li>Projects</li>
 *  <li>Education</li>
 *  <li>Languages</li>
 * </ol>
 * Кожна секція додається тільки якщо в ній є дані. Inline skills/languages
 * (одним рядком через коми) — компактніше за списком.
 */
@Component
@RequiredArgsConstructor
public class CvDocxTemplate {
 
    
    private static final DateTimeFormatter PERIOD_FORMAT =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final DocxSectionBuilder builder;
 
    /**
     * Заповнює документ. Викликається з {@link DocxExporter}.
     */
    public void build(XWPFDocument doc, TailoredCvResponse cv) {
        addHeroBlock(doc, cv);
        addSummary(doc, cv);
        addExperience(doc, cv);
        addSkills(doc, cv);
        addProjects(doc, cv);
        addEducation(doc, cv);
        addLanguages(doc, cv);
    }
 
    
 
    private void addHeroBlock(XWPFDocument doc, TailoredCvResponse cv) {
        if (notBlank(cv.getFullName())) {
            builder.nameTitle(doc, cv.getFullName());
        }
        if (notBlank(cv.getHeadline())) {
            builder.headline(doc, cv.getHeadline());
        }
        String contact = buildContactLine(cv);
        if (notBlank(contact)) {
            builder.contactLine(doc, contact);
        }
        builder.blankLine(doc);
    }
 
    private void addSummary(XWPFDocument doc, TailoredCvResponse cv) {
        if (!notBlank(cv.getSummary())) return;
 
        builder.sectionHeading(doc, "Summary");
        builder.bodyLine(doc, cv.getSummary());
        builder.blankLine(doc);
    }
 
    private void addExperience(XWPFDocument doc, TailoredCvResponse cv) {
        List<TailoredExperienceResponse> items = cv.getExperiences();
        if (items == null || items.isEmpty()) return;
 
        builder.sectionHeading(doc, "Experience");
        for (TailoredExperienceResponse e : items) {
            builder.itemHeading(doc, formatPositionLine(e));
            builder.metaLine(doc, formatExperienceMeta(e));
 
            // Description: рядки розділені \n — кожен як bullet point.
            if (notBlank(e.getDescription())) {
                for (String line : e.getDescription().split("\n")) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        builder.bulletItem(doc, trimmed);
                    }
                }
            }
            builder.blankLine(doc);
        }
    }
 
    private void addSkills(XWPFDocument doc, TailoredCvResponse cv) {
        List<TailoredSkillResponse> items = cv.getSkills();
        if (items == null || items.isEmpty()) return;
 
        builder.sectionHeading(doc, "Skills");
        // Inline: "java, spring boot, postgresql, docker"
        String inline = items.stream()
                .map(TailoredSkillResponse::getName)
                .collect(Collectors.joining(", "));
        builder.bodyLine(doc, inline);
        builder.blankLine(doc);
    }
 
    private void addProjects(XWPFDocument doc, TailoredCvResponse cv) {
        List<TailoredProjectResponse> items = cv.getProjects();
        if (items == null || items.isEmpty()) return;
 
        builder.sectionHeading(doc, "Projects");
        for (TailoredProjectResponse p : items) {
            // "Job Tracker [Java, Spring, PostgreSQL]"
            String heading = p.getName();
            if (notBlank(p.getTechnologies())) {
                heading += " [" + p.getTechnologies() + "]";
            }
            builder.itemHeading(doc, heading);
 
            if (notBlank(p.getUrl())) {
                builder.metaLine(doc, p.getUrl());
            }
            if (notBlank(p.getDescription())) {
                builder.bodyLine(doc, p.getDescription());
            }
            builder.blankLine(doc);
        }
    }
 
    private void addEducation(XWPFDocument doc, TailoredCvResponse cv) {
        List<TailoredEducationResponse> items = cv.getEducations();
        if (items == null || items.isEmpty()) return;
 
        builder.sectionHeading(doc, "Education");
        for (TailoredEducationResponse e : items) {
            String degreeLine = formatDegreeLine(e);
            if (notBlank(degreeLine)) {
                builder.itemHeading(doc, degreeLine);
            }
            builder.metaLine(doc, formatEducationMeta(e));
            builder.blankLine(doc);
        }
    }
 
    private void addLanguages(XWPFDocument doc, TailoredCvResponse cv) {
        List<TailoredLanguageResponse> items = cv.getLanguages();
        if (items == null || items.isEmpty()) return;
 
        builder.sectionHeading(doc, "Languages");
        // "English (C1), Polish (NATIVE)"
        String inline = items.stream()
                .map(l -> l.getName() + " (" + l.getLevel() + ")")
                .collect(Collectors.joining(", "));
        builder.bodyLine(doc, inline);
    }
 
    
 
    /** "Senior Java Developer @ Acme Corp" або просто "Senior Java Developer" якщо нема компанії. */
    private String formatPositionLine(TailoredExperienceResponse e) {
        if (notBlank(e.getCompany())) {
            return e.getPosition() + " @ " + e.getCompany();
        }
        return e.getPosition();
    }
 
    /** "Jan 2022 – Present | Warsaw, Poland". */
    private String formatExperienceMeta(TailoredExperienceResponse e) {
        String period = formatPeriod(e.getStartDate(), e.getEndDate());
        return notBlank(e.getLocation()) ? period + " | " + e.getLocation() : period;
    }
 
    /** "Bachelor in Computer Science" або просто degree чи field якщо одне з них null. */
    private String formatDegreeLine(TailoredEducationResponse e) {
        boolean hasDegree = notBlank(e.getDegree());
        boolean hasField = notBlank(e.getFieldOfStudy());
        if (hasDegree && hasField) return e.getDegree() + " in " + e.getFieldOfStudy();
        if (hasDegree) return e.getDegree();
        if (hasField) return e.getFieldOfStudy();
        return "";
    }
 
    /** "Warsaw University | Sep 2018 – Jun 2022". */
    private String formatEducationMeta(TailoredEducationResponse e) {
        String period = formatPeriod(e.getStartDate(), e.getEndDate());
        if (notBlank(e.getInstitution()) && !period.isEmpty()) {
            return e.getInstitution() + " | " + period;
        }
        if (notBlank(e.getInstitution())) return e.getInstitution();
        return period;
    }
 
    /** "Jan 2022 – Present" або "Jan 2022 – Jun 2023". null start → пустий рядок. */
    private String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null) return "";
        String startStr = start.format(PERIOD_FORMAT);
        String endStr = end != null ? end.format(PERIOD_FORMAT) : "Present";
        return startStr + " – " + endStr;
    }
 
    /** "john@example.com | +48 123 456 789 | linkedin.com/in/john | github.com/john". */
    private String buildContactLine(TailoredCvResponse cv) {
        List<String> parts = new ArrayList<>();
        if (notBlank(cv.getEmail())) parts.add(cv.getEmail());
        if (notBlank(cv.getPhone())) parts.add(cv.getPhone());
        if (notBlank(cv.getLinkedInUrl())) parts.add(cv.getLinkedInUrl());
        if (notBlank(cv.getGithubUrl())) parts.add(cv.getGithubUrl());
        return String.join("  |  ", parts);
    }
 
    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}

