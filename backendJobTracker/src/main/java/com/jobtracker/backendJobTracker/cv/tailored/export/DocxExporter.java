package com.jobtracker.backendJobTracker.cv.tailored.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import com.jobtracker.backendJobTracker.cv.tailored.dto.TailoredCvResponse;

import lombok.RequiredArgsConstructor;


/**
 * Публічний API експорту. Контролер інжектить ЦЕЙ клас — не Template, не Builder.
 * <p>
 * <b>Flow:</b>
 * <ol>
 *  <li>Створюємо порожній XWPFDocument (= порожній .docx файл у пам'яті)</li>
 *  <li>Делегуємо template'у наповнити документ секціями</li>
 *  <li>Записуємо документ у byte stream</li>
 *  <li>Повертаємо byte[] — контролер віддасть як attachment у HTTP response</li>
 * </ol>
 * <p>
 * <b>Чому byte[] а не File / Path</b>: на сервері не зберігаємо файли. CV
 * генерується on-demand при запиті download. Менше disk I/O, менше cleanup,
 * легше масштабувати.
 */
@Component
@RequiredArgsConstructor
public class DocxExporter {
 
    private final CvDocxTemplate template;
 
    /**
     * Генерує DOCX як байтовий масив.
     * <p>
     * <b>try-with-resources:</b> XWPFDocument і ByteArrayOutputStream обидва
     * AutoCloseable. JVM закриває їх у зворотному порядку оголошення (out → doc)
     * автоматично, навіть якщо виникне виняток. Це важливо для XWPFDocument —
     * без close() лишаються відкриті ресурси (тимчасові файли в OPC package).
     */
    public byte[] export(TailoredCvResponse cv) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 
            // 1. Наповнюємо документ — через template, який під капотом
            //    викликає DocxSectionBuilder для кожної секції.
            template.build(doc, cv);
 
            // 2. POI серіалізує документ у OOXML формат і пише у byte stream.
            //    Це не "збереження файлу" — просто записування байтів куди скажемо.
            doc.write(out);
 
            // 3. Витягуємо байти. toByteArray() копіює внутрішній буфер.
            //    Можна викликати навіть після close (буфер у пам'яті лишається).
            return out.toByteArray();
 
        } catch (IOException e) {
            // IOException теоретично можливий навіть при write у ByteArrayOutputStream
            // (через POI internal logic), хоч практично рідкісний.
            throw new ExportException("Failed to export tailored CV as DOCX", e);
        }
    }
}

