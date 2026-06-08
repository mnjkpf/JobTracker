package com.jobtracker.backendJobTracker.application.parsing;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.application.enums.ContractType;
import com.jobtracker.backendJobTracker.application.enums.Seniority;
import com.jobtracker.backendJobTracker.application.enums.SourceBoard;
import com.jobtracker.backendJobTracker.application.enums.WorkMode;
import com.jobtracker.backendJobTracker.application.parsing.dto.ParsedJobPosting;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class PracujParser implements JobBoardParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracujParser.class);

    // Selectors are kept at the top so they are easy to update when Pracuj changes HTML.
    private static final String SELECTOR_POSITION = "h1[data-test='text-positionName'], [data-test='text-positionName'], h1";
    private static final String SELECTOR_COMPANY = "[data-test='text-employerName'], [data-test='text-companyName'], a[href*='pracodawcy.pracuj.pl']";
    private static final String SELECTOR_LOCATION = "[data-test='text-region'], [data-test='text-location'], [data-test='section-offer-location']";
    private static final String SELECTOR_DESCRIPTION = "[data-test='section-responsibilities'], [data-test='section-description'], [data-test='section-about-company']";
    private static final String SELECTOR_SALARY = "[data-test*='salary'], [data-test*='Salary'], [data-test='text-salary']";
    private static final String SELECTOR_CONTRACT = "[data-test='section-contract-type'], [data-test='section-employment-type-name-text']";
    private static final String SELECTOR_WORK_MODE = "[data-test='section-work-mode']";
    private static final String SELECTOR_SENIORITY = "[data-test='section-position-level']";

    @Override
    public SourceBoard supportedBoard() {
        return SourceBoard.PRACUJ;
    }

    @Override
    public Optional<ParsedJobPosting> parse(String html, String url) {
        Document doc = Jsoup.parse(html);

        String position = cleanPosition(tryExtract(doc, SELECTOR_POSITION));
        String company = tryExtract(doc, SELECTOR_COMPANY);

        if (position == null || company == null) {
            LOGGER.warn("Missing required fields in PRACUJ HTML: {}", url);
            return Optional.empty();
        }

        String location = tryExtract(doc, SELECTOR_LOCATION);
        String description = tryExtract(doc, SELECTOR_DESCRIPTION);

        SalaryRange salary = parseSalary(doc);
        ContractType contractType = parseContractType(doc);
        WorkMode workMode = parseWorkMode(doc);
        Seniority seniority = parseSeniority(doc, url);

        return Optional.of(ParsedJobPosting.builder()
                .position(position)
                .companyName(company)
                .description(description)
                .location(location)
                .salaryMin(salary != null ? salary.min : null)
                .salaryMax(salary != null ? salary.max : null)
                .salaryCurrency(salary != null ? salary.currency : null)
                .contractType(contractType)
                .workMode(workMode)
                .seniority(seniority)
                .parsedByLlm(false)
                .build());
    }

    private String tryExtract(Document doc, String selector) {
        Elements elements = doc.select(selector);
        if (elements.isEmpty()) return null;
        String text = elements.first().text().trim();
        return text.isBlank() ? null : text;
    }

    private SalaryRange parseSalary(Document doc) {
        String raw = tryExtract(doc, SELECTOR_SALARY);
        if (raw == null) return null;

        String normalized = normalize(raw);
        Pattern p = Pattern.compile("(\\d[\\d\\s.,]*)\\s*(?:-|\\p{Pd}|do)\\s*(\\d[\\d\\s.,]*)\\s*(pln|zl|eur|usd)");
        Matcher m = p.matcher(normalized);
        if (!m.find()) return null;

        try {
            int min = Integer.parseInt(m.group(1).replaceAll("[^0-9]", ""));
            int max = Integer.parseInt(m.group(2).replaceAll("[^0-9]", ""));
            return new SalaryRange(min, max, parseCurrency(m.group(3)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ContractType parseContractType(Document doc) {
        String raw = tryExtract(doc, SELECTOR_CONTRACT);
        if (raw == null) return null;

        String lower = normalize(raw);
        if (lower.contains("b2b")) return ContractType.B2B;
        if (lower.contains("umowa o prace") || lower.contains("uop")) return ContractType.UOP;
        if (lower.contains("zlecenie")) return ContractType.UZ;
        if (lower.contains("dzielo")) return ContractType.UMOWA_O_DZIELO;
        return ContractType.NOT_SPECIFIED;
    }

    private WorkMode parseWorkMode(Document doc) {
        String raw = tryExtract(doc, SELECTOR_WORK_MODE);
        if (raw == null) return null;

        String lower = normalize(raw);
        if (lower.contains("zdalna") || lower.contains("remote")) return WorkMode.REMOTE;
        if (lower.contains("hybryd")) return WorkMode.HYBRID;
        if (lower.contains("stacjon") || lower.contains("onsite")) return WorkMode.ONSITE;
        return WorkMode.NOT_SPECIFIED;
    }

    private Seniority parseSeniority(Document doc, String url) {
        String raw = firstNonBlank(tryExtract(doc, SELECTOR_SENIORITY), url);
        if (raw == null) return null;

        String lower = normalize(raw);
        if (lower.contains("lead")) return Seniority.LEAD;
        if (lower.contains("senior") || lower.contains("starszy specjalista")) return Seniority.SENIOR;
        if (lower.contains("junior plus")) return Seniority.JUNIOR_PLUS;
        if (lower.contains("junior") || lower.contains("mlodszy specjalista")) return Seniority.JUNIOR;
        if (lower.contains("mid") || lower.contains("regular")) return Seniority.MID;
        if (lower.contains("intern") || lower.contains("praktykant") || lower.contains("stazysta")) return Seniority.INTERN;
        return null;
    }

    private String cleanPosition(String raw) {
        if (raw == null) return null;
        String lower = normalize(raw);
        if ("praca".equals(lower) || lower.startsWith("praca:") || lower.startsWith("oferty pracy")) {
            return null;
        }
        return raw.trim();
    }

    private String parseCurrency(String raw) {
        String lower = normalize(raw);
        if (lower.contains("zl") || lower.contains("pln")) return "PLN";
        if (lower.contains("eur")) return "EUR";
        if (lower.contains("usd")) return "USD";
        return raw.toUpperCase(Locale.ROOT);
    }

    private String normalize(String raw) {
        String text = raw.replace('\u00A0', ' ').trim();
        String withoutDiacritics = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase(Locale.ROOT);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    private record SalaryRange(int min, int max, String currency) {}
}
