package com.jobtracker.backendJobTracker.cv;

import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CvFileParser {

    // Логер — пише повідомлення в консоль/лог-файл (для дебагу та помилок)
    private final static Logger LOGGER = LoggerFactory.getLogger(CvFileParser.class);

    // Tika — бібліотека, що дістає звичайний текст із файлів (PDF, DOCX тощо),
    // не залежно від формату. Тут використовуємо, щоб витягнути текст із CV.
    private final Tika tike  =new Tika();

    // Обмеження: максимум символів, які читаємо з CV (захист від величезних файлів)
    private final static int MAX_CHARS_CV = 1_000_000;

    static{

    }

    public String extractText(MultipartFile file) {
        // try-with-resources: потік (InputStream) автоматично закриється в кінці
        try(InputStream in = file.getInputStream()){
            // parseToString читає файл і повертає текст.
            // new Metadata() — контейнер для метаданих файлу (назва, тип, автор тощо).
            // Tika заповнює його під час парсингу; тут він обов'язковий аргумент,
            // навіть якщо самі метадані нам поки не потрібні.
            String text = tike.parseToString(in,new  Metadata(),MAX_CHARS_CV);
            // Якщо текст порожній — у файлі нема що читати (наприклад, скан-картинка)
            if (text == null || text.isBlank()) {
                throw new CvExtractionException("File contains no extractable text.", null);
            }
            LOGGER.debug("Extracted {} chars from {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (Exception e) {
            // Будь-яка помилка читання/парсингу -> своя зрозуміла бізнес-помилка
            LOGGER.error("Failed to extract text from CV file: {}", e.getMessage());
            throw new CvExtractionException("Failed to parse CV file", e);
        }
    }
}
