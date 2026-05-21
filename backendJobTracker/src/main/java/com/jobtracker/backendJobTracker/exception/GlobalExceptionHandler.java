// Оголошення пакету — вказує, де у структурі проєкту лежить цей клас
package com.jobtracker.backendJobTracker.exception;

// Імпорт HttpServletRequest — об'єкт HTTP-запиту, з якого можемо отримати URI, заголовки тощо
import jakarta.servlet.http.HttpServletRequest;
// Імпорт інтерфейсу логера SLF4J — стандарт логування у Spring
import org.slf4j.Logger;
// Фабрика для створення екземпляра логера
import org.slf4j.LoggerFactory;
// Enum зі стандартними HTTP-статусами (404, 409, 401, 422, 500 і т.д.)
import org.springframework.http.HttpStatus;
// Клас Spring для формування відповіді у форматі RFC 7807 (Problem Details for HTTP APIs)
import org.springframework.http.ProblemDetail;
// Обгортка HTTP-відповіді: дозволяє задати статус, заголовки та тіло
import org.springframework.http.ResponseEntity;
// Анотація, що позначає метод як обробник конкретного типу винятку
import org.springframework.web.bind.annotation.ExceptionHandler;
// Анотація, що робить клас глобальним обробником винятків для всіх @RestController
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Клас URI — використовується для полів type та instance у ProblemDetail
import java.net.URI;
// Клас часу в UTC у форматі ISO-8601 — для поля timestamp
import java.time.Instant;

// @RestControllerAdvice — Spring сканує цей клас та підключає його обробники до всіх REST-контролерів у застосунку
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Логер для цього класу — пише повідомлення про помилки у консоль/файли логів
    // static final — один екземпляр на клас, не змінюється під час роботи
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @ExceptionHandler(...) — метод буде викликаний, коли в будь-якому контролері кинуть ResourceNotFoundException
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            // ex — сам виняток, з нього беремо повідомлення про причину
            // request — HTTP-запит, з нього беремо шлях, на якому виникла помилка
            ResourceNotFoundException ex, HttpServletRequest request) {
        // Пишемо у лог попередження (warn) — це очікувана помилка, не критична
        // {} — плейсхолдери, які підставляться значеннями параметрів після рядка-шаблону
        log.warn("Resource not found: {} (path={})", ex.getMessage(), request.getRequestURI());
        // Делегуємо побудову відповіді приватному методу build() — щоб не дублювати код
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", "RESOURCE_NOT_FOUND", ex, request);
    }

    // Обробник конфліктів (наприклад, спроба створити користувача з email, який вже існує)
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflictException(
            ConflictException ex, HttpServletRequest request) {
        // Логуємо як warn — конфлікт це нормальна частина бізнес-логіки, не баг
        log.warn("Conflict: {} (path={})", ex.getMessage(), request.getRequestURI());
        // Повертаємо HTTP 409 Conflict із деталями
        return build(HttpStatus.CONFLICT, "Conflict", "CONFLICT", ex, request);
    }

    // Обробник помилок авторизації — користувач не залогінений або токен невалідний
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        // Логуємо як warn — спроба несанкціонованого доступу
        log.warn("Unauthorized access: {} (path={})", ex.getMessage(), request.getRequestURI());
        // Повертаємо HTTP 401 Unauthorized
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "UNAUTHORIZED", ex, request);
    }

    // Обробник порушень бізнес-правил (наприклад, "не можна подати заявку після дедлайну")
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleException(
            BusinessRuleException ex, HttpServletRequest request) {
        log.warn("Business rule violation: {} (path={})", ex.getMessage(), request.getRequestURI());
        // 422 Unprocessable Entity — запит синтаксично правильний, але порушує бізнес-правило
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", "BUSINESS_RULE_VIOLATION", ex, request);
    }

    // "Запобіжник" — ловить усі непередбачені винятки (NullPointerException, помилки БД тощо)
    // Спрацьовує тільки якщо жоден специфічний хендлер вище не підійшов
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception ex, HttpServletRequest request) {
        // log.error + третій аргумент ex — записує повний стек-трейс у лог для розслідування
        log.error("Unexpected error (path={})", request.getRequestURI(), ex);
        // Створюємо ProblemDetail зі статусом 500 і узагальненим повідомленням
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                // ВАЖЛИВО: не показуємо ex.getMessage() користувачу — щоб не зливати деталі реалізації/секрети
                "An unexpected error occurred. Please contact support if the problem persists.");
        // Заголовок проблеми — людиночитна короткa назва
        problem.setTitle("Internal Server Error");
        // type — URI на документацію цього типу помилки (можна вести на свою сторінку з описом)
        problem.setType(URI.create("https://jobtracker.local/errors/INTERNAL_ERROR"));
        // instance — URI конкретного запиту, де сталася помилка (допомагає у налагодженні)
        problem.setInstance(URI.create(request.getRequestURI()));
        // Кастомні поля — додаємо машиночитний код помилки для клієнта
        problem.setProperty("errorCode", "INTERNAL_ERROR");
        // Часова мітка у UTC — корисно для співставлення з логами
        problem.setProperty("timestamp", Instant.now().toString());
        // Збираємо ResponseEntity: статус 500 + тіло ProblemDetail
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // Приватний хелпер — щоб не повторювати однакові 7 рядків у кожному хендлері (DRY)
    private ResponseEntity<ProblemDetail> build(
            // status — який HTTP-статус повернути (404/409/401/422)
            HttpStatus status,
            // title — людиночитний заголовок ("Resource Not Found")
            String title,
            // errorCode — машиночитний код для клієнта ("RESOURCE_NOT_FOUND")
            String errorCode,
            // ex — оригінальний виняток, з якого беремо detail-повідомлення
            Exception ex,
            // request — щоб дістати шлях запиту
            HttpServletRequest request) {
        // Створюємо ProblemDetail: статус + detail (повідомлення з винятку)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        // Встановлюємо заголовок
        problem.setTitle(title);
        // type — URI на документацію типу помилки, унікальний для кожного errorCode
        problem.setType(URI.create("https://jobtracker.local/errors/" + errorCode));
        // instance — конкретний запит, який зламався
        problem.setInstance(URI.create(request.getRequestURI()));
        // Машиночитний код — клієнт може на нього орієнтуватись у коді (switch/case)
        problem.setProperty("errorCode", errorCode);
        // Час події — для кореляції з логами на сервері
        problem.setProperty("timestamp", Instant.now().toString());
        // Формуємо фінальну відповідь: HTTP-статус + тіло у форматі application/problem+json
        return ResponseEntity.status(status).body(problem);
    }
}
