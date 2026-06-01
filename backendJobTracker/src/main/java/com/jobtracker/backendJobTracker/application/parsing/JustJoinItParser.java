package com.jobtracker.backendJobTracker.application.parsing;


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
public class JustJoinItParser implements JobBoardParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(JustJoinItParser.class);

    // Селектори зверху — легко знайти і поправити коли сайт зміниться
    private static final String SELECTOR_POSITION = "h1.posting-title";
    private static final String SELECTOR_COMPANY = "a.company-name";
    private static final String SELECTOR_LOCATION = "span.location";
    private static final String SELECTOR_DESCRIPTION = "div.job-description";


    @Override
    public SourceBoard supportedBoard() {
        return SourceBoard.JUSTJOINIT;
    }

    @Override
    public Optional<ParsedJobPosting> parse(String html, String url) {
        Document doc = Jsoup.parse(html);

        String position = tryExtract(doc, SELECTOR_POSITION);
        String company = tryExtract(doc, SELECTOR_COMPANY);

        if(position == null || company == null) {
            LOGGER.warn("Missing required fields in JUSTJOINIT HTML: {}", url);
            return Optional.empty();
        }

        String location = tryExtract(doc, SELECTOR_LOCATION);
        String description = tryExtract(doc, SELECTOR_DESCRIPTION);

        SalaryRange salary = parseSalary(doc);
        ContractType contractType = parseContractType(doc);
        WorkMode workMode = parseWorkMode(doc);
        Seniority seniority = parseSeniorityFromUrl(url);  // часто є у slug

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
                .parsedByLlm(false)   // важливо: не через LLM!
                .build());

    }

    private String tryExtract(Document doc,String selector){
        Elements elements = doc.select(selector);
        if(elements.isEmpty()) return null;
        String text = elements.first().text().trim();
        return text.isBlank() ? null : text;
    }

    private SalaryRange parseSalary(Document doc){
        String raw = tryExtract(doc, "span.salary-range");
        if (raw == null) return null;
        Pattern p = Pattern.compile("(\\d[\\d\\s]+)\\s*-\\s*(\\d[\\d\\s]+)\\s*(PLN|EUR|USD)");
        Matcher m = p.matcher(raw);
        if (!m.find()) return null;
        try {
            int min = Integer.parseInt(m.group(1).replaceAll("\\s", ""));
            int max = Integer.parseInt(m.group(2).replaceAll("\\s", ""));
            return new SalaryRange(min, max, m.group(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ContractType parseContractType(Document doc) {
        String raw = tryExtract(doc, "span.contract-type");
        if (raw == null) return null;
        String lower = raw.toLowerCase();
        if(lower.contains("b2b")) return ContractType.B2B;
        if(lower.contains("o pracę") || lower.contains("uop")) return ContractType.UOP;
        if(lower.contains("zlecenie")) return ContractType.UZ;
        return ContractType.NOT_SPECIFIED;
    }


    private WorkMode parseWorkMode(Document doc) {
        String raw = tryExtract(doc, "span.work-mode");
        if (raw == null) return null;
        String lower = raw.toLowerCase();
        if(lower.contains("zdalna") || lower.contains("remote")) return WorkMode.REMOTE;
        if(lower.contains("hybryd")) return WorkMode.HYBRID;
        if(lower.contains("stacjon") || lower.contains("onsite")) return WorkMode.ONSITE;
        return WorkMode.NOT_SPECIFIED;
    }

    private Seniority parseSeniorityFromUrl(String url){
        String lower = url.toLowerCase();
        if (lower.contains("junior")) return Seniority.JUNIOR;
        if (lower.contains("mid")) return Seniority.MID;
        if (lower.contains("senior")) return Seniority.SENIOR;
        return null;
    }

    private record SalaryRange(int min, int max, String currency) {}
}
