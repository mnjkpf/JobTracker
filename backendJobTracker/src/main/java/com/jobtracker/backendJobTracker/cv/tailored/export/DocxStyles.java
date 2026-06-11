package com.jobtracker.backendJobTracker.cv.tailored.export;


public final class DocxStyles {
 
    private DocxStyles() {
    }
 
    
 
    /** Базовий шрифт всього документа. Calibri — універсальний з підтримкою діакритик. */
    public static final String FONT_FAMILY = "Calibri";
 
    
    /** Ім'я кандидата зверху сторінки — найбільший текст. */
    public static final int FONT_SIZE_NAME = 22;
 
    /** Headline під іменем — як "Senior Java Developer". */
    public static final int FONT_SIZE_HEADLINE = 11;
 
    /** Назви секцій: SUMMARY, EXPERIENCE, тощо. */
    public static final int FONT_SIZE_SECTION_HEADING = 14;
 
    /** Заголовок item — position у experience, degree у education. */
    public static final int FONT_SIZE_ITEM_HEADING = 11;
 
    /** Основний body текст (description, summary). */
    public static final int FONT_SIZE_BODY = 10;
 
    /** Метаінформація (дати, локація). Менше і блідіше. */
    public static final int FONT_SIZE_META = 9;
 
    
 
    /** Основний колір тексту. Темно-сірий замість суто чорного — м'якіше для очей. */
    public static final String COLOR_BODY = "2C3E50";
 
    /** Колір section headings — темно-синій, формальний. */
    public static final String COLOR_HEADING = "1F4E79";
 
    /** Колір метаінформації — світло-сірий. */
    public static final String COLOR_META = "707070";
 
    
    public static final String BULLET = "•";
}

