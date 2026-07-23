package com.jobtracker.backendJobTracker.cv.tailored.export;

import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.springframework.stereotype.Component;

/**
 * Helpers для побудови секцій DOCX. Обгортка над verbose POI API.
 * <p>
 * <b>Чому ці хелпери потрібні</b>: голий POI має багатослівний API:
 * <pre>
 * XWPFParagraph p = doc.createParagraph();
 * p.setAlignment(ParagraphAlignment.CENTER);
 * XWPFRun r = p.createRun();
 * r.setFontFamily("Calibri");
 * r.setFontSize(22);
 * r.setBold(true);
 * r.setColor("1F4E79");
 * r.setText("John Doe");
 * </pre>
 * <p>
 * Це 7 рядків для одного title. У нас 9 типів елементів (name, headline, contact,
 * sectionHeading, itemHeading, body, meta, bullet, blank), 4-5 викликів кожен у
 * template — без хелперів було б 200+ рядків boilerplate.
 * <p>
 * <b>Концепція POI у двох словах:</b>
 * <ul>
 * <li><b>XWPFDocument</b> — цілий .docx файл у пам'яті</li>
 * <li><b>XWPFParagraph</b> — один параграф (≈ &lt;p&gt; у HTML).
 *     Контролює alignment, indent, borders.</li>
 * <li><b>XWPFRun</b> — частина тексту з власним стилем (≈ &lt;span&gt;).
 *     Контролює font family, size, color, bold/italic. Один параграф може
 *     мати кілька runs з різними стилями.</li>
 * </ul>
 */
@Component
public class DocxSectionBuilder {
 
    
 
    /**
     * Велике центроване ім'я кандидата зверху сторінки.
     * Bold, 22pt, body color.
     */
    public void nameTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_NAME, true, DocxStyles.COLOR_BODY);
        r.setText(text);
    }
 
    /**
     * Headline під іменем — "Senior Java Developer". Менший, не bold, gray.
     */
    public void headline(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_HEADLINE, false, DocxStyles.COLOR_META);
        r.setText(text);
    }
 
    /**
     * Один рядок з контактною інформацією, центровано.
     * Наприклад: "john@example.com | +48 123 456 789 | linkedin.com/in/john"
     */
    public void contactLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_META, false, DocxStyles.COLOR_META);
        r.setText(text);
    }
 
    
 
    /**
     * Bold кольоровий заголовок секції З НИЖНЬОЮ ЛІНІЄЮ — візуальний розділювач.
     * <p>
     * <b>Нижня лінія — це найскладніша частина POI у цьому файлі.</b>
     * POI не має методу типу "setBottomBorder()" — треба лізти у нижньолежне XML.
     * Розшифрую за коментарями нижче.
     */
    public void sectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
 
        // Спочатку нормальний run — bold, кольорова назва секції
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_SECTION_HEADING, true, DocxStyles.COLOR_HEADING);
        r.setText(text.toUpperCase());     // SUMMARY, EXPERIENCE — більше формально
 
        // ──────────── Нижня лінія через "сирий" XML ────────────
        //
        // XWPFParagraph — це high-level Java wrapper. Під ним сидить
        // CTP (Compex Type Paragraph) — згенерований клас з XML schema.
        // CTPPr (Paragraph Properties) — секція "налаштування параграфа".
        // CTPBdr (Paragraph Borders) — підсекція "межі параграфа".
        // CTBorder — окрема межа (top/bottom/left/right).
        //
        // Назви такі "сирі" бо це автогенеровані Java-класи з .xsd файлу
        // OOXML стандарту. Назви CTPBdr/CTPPr — це скорочення прямо з XML
        // (наприклад <w:pBdr>, <w:pPr>). Виглядає страшно, але це просто
        // type-safe доступ до того самого XML що Word зберігає у .docx.
 
        CTPPr pPr = ensurePPr(p);                   // отримати/створити <w:pPr>
        CTPBdr borders = pPr.isSetPBdr()             // отримати/створити <w:pBdr>
                ? pPr.getPBdr()
                : pPr.addNewPBdr();
 
        CTBorder bottom = borders.isSetBottom()       // отримати/створити <w:bottom>
                ? borders.getBottom()
                : borders.addNewBottom();
 
        bottom.setVal(STBorder.SINGLE);                                       // тип лінії
        bottom.setSz(BigInteger.valueOf(6));                                  // товщина в eighths of a point (6 = ¾pt)
        bottom.setSpace(BigInteger.valueOf(1));                               // відстань між текстом і лінією
        bottom.setColor(DocxStyles.COLOR_HEADING);
    }
 
    
 
    /**
     * Bold заголовок одного item у секції — "Senior Developer @ Acme Corp".
     */
    public void itemHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_ITEM_HEADING, true, DocxStyles.COLOR_BODY);
        r.setText(text);
    }
 
    
 
    /**
     * Нормальний абзац — summary, description.
     * <p>
     * <b>Підтримка \n у тексті</b>: POI Run.setText() не обробляє переноси рядків
     * автоматично. Треба викликати addBreak() між шматками тексту. Цей хелпер
     * це робить — split по \n + addBreak.
     */
    public void bodyLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_BODY, false, DocxStyles.COLOR_BODY);
        addMultilineRun(r, text);
    }
 
    /**
     * Маленький сірий текст — дати, локація. Часто йде під item heading.
     */
    public void metaLine(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_META, false, DocxStyles.COLOR_META);
        r.setItalic(true);     // курсив для метаданих — більш elegant
        r.setText(text);
    }
 
    /**
     * Bullet point — рядок з префіксом "• " і лівим відступом.
     * <p>
     * <b>Чому не справжній POI bulleting?</b> Справжні bullet lists у POI
     * вимагають створення XWPFNumbering definition і прив'язки. Це 30+ рядків
     * boilerplate. Простий хак "• " + indent виглядає так само у Word і працює
     * скрізь (LibreOffice, Google Docs).
     */
    public void bulletItem(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
 
        // 360 twips = ¼ inch left indent. Twip = 1/1440 inch.
        // Word внутрішньо вимірює все у twips. setIndentationLeft приймає twips напряму.
        p.setIndentationLeft(360);
 
        XWPFRun r = p.createRun();
        applyFont(r, DocxStyles.FONT_SIZE_BODY, false, DocxStyles.COLOR_BODY);
        r.setText(DocxStyles.BULLET + "  " + text);
    }
 
    /**
     * Порожній рядок для візуального розділення секцій.
     * Просто створює параграф без runs (без тексту, без runs = чиста порожнина).
     */
    public void blankLine(XWPFDocument doc) {
        doc.createParagraph();
    }
 
    
 
    /**
     * Консолідований апплай шрифтових налаштувань на один run.
     * Без цього довелось би в кожному методі писати 4 setter'и.
     */
    private void applyFont(XWPFRun r, int size, boolean bold, String color) {
        r.setFontFamily(DocxStyles.FONT_FAMILY);
        r.setFontSize(size);
        r.setBold(bold);
        r.setColor(color);
    }
 
    /**
     * Додає текст з підтримкою \n переносів через POI's addBreak().
     * <p>
     * Приклад: text = "Line 1\nLine 2\nLine 3"
     * Результат: три рядки у тому самому параграфі (м'які переноси, не нові
     * параграфи).
     */
    private void addMultilineRun(XWPFRun r, String text) {
        if (text == null) return;
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) r.addBreak();          // <w:br/> у XML — soft line break
            r.setText(lines[i], i);           // index — позиція у run (для multi-text)
        }
    }
 
    /**
     * Дістає або створює CTPPr — "Paragraph Properties" контейнер.
     * <p>
     * CTPPr тримає налаштування параграфа: alignment, indent, borders, spacing.
     * Якщо параграф ще не має пропертей (тільки що створений) — створюємо їх.
     * Інакше повертаємо існуючий, щоб не перезаписати чужі налаштування.
     */
    private CTPPr ensurePPr(XWPFParagraph p) {
        return p.getCTP().isSetPPr()
                ? p.getCTP().getPPr()
                : p.getCTP().addNewPPr();
    }

}
