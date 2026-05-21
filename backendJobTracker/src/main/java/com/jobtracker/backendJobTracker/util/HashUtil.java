
package com.jobtracker.backendJobTracker.util;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/*
 * HashUtil — утиліта для криптографічного хешування за алгоритмом SHA-256.
 *
 * ВАЖЛИВО ПРО БЕЗПЕКУ:
 *   SHA-256 — це БЕЗ солі та БЕЗ повторень. Він НЕ підходить для хешування паролів!
 *   Для паролів використовуй BCrypt / Argon2 / PBKDF2 (через Spring Security PasswordEncoder).
 *   Цей клас доречний для:
 *     - хешування refresh-токенів перед збереженням у БД (щоб у разі витоку БД токени не можна було використати);
 *     - перевірки цілісності даних (порівняння двох рядків через хеш);
 *     - детермінованих ідентифікаторів (один вхід → один вихід).
 */
public class HashUtil {

    // Назва алгоритму винесена у константу — щоб не дублювати літерал і легко змінити в одному місці
    // private — не доступна ззовні класу
    // static final — одна на весь клас, незмінна
    private static final String ALGORITHM = "SHA-256";

    // Приватний конструктор — забороняє створювати екземпляри цього класу через new HashUtil()
    // Це т.з. "utility class pattern": усі методи статичні, об'єкт не потрібен.
    // Якби конструктор був публічним за замовчуванням, хтось міг би помилково написати "new HashUtil()" і збити з пантелику читача.
    private HashUtil() {
        // Тіло порожнє — конструктор не повинен викликатися ніколи
    }

    /*
     * sha256(input) — обчислює SHA-256 хеш від рядка та повертає його у HEX-форматі.
     *
     * static — викликається через клас: HashUtil.sha256("abc")
     * Повертає String довжиною 64 символи (256 біт / 4 біти на hex-символ = 64).
     */
    public static String sha256(String input){
        // Захист від NPE — якщо викликати digest на null, отримаємо незрозумілий NullPointerException
        // Краще одразу кинути зрозумілий виняток із поясненням
        if(input == null) {
            // IllegalArgumentException — стандартний виняток для "невалідний параметр методу"
            throw new IllegalArgumentException("Input cannot be null");
        }

        // try/catch обов'язковий, бо MessageDigest.getInstance() оголошений як checked-throws
        try{
            // Отримуємо екземпляр SHA-256 через фабрику JDK
            // Кожен виклик повертає НОВИЙ екземпляр — MessageDigest НЕ потокобезпечний,
            // тому не зберігаємо його у static-полі, а створюємо при кожному виклику
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

            // Два кроки в одному рядку:
            //   1) input.getBytes(UTF_8) — перетворюємо String у byte[] з фіксованим кодуванням
            //   2) digest.digest(bytes) — обчислюємо хеш, повертає byte[] довжиною 32 байти (256 біт)
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // HexFormat.of() — отримуємо форматер за замовчуванням (нижній регістр, без розділювачів)
            // .formatHex(hash) — конвертуємо byte[] у HEX-рядок "a1b2c3..."
            // Повертаємо результат як значення методу
            return HexFormat.of().formatHex(hash);

        }  catch (NoSuchAlgorithmException e) {
            // SHA-256 присутній у кожній JVM, але контракт вимагає обробки
            // Загортаємо checked-виняток у unchecked (IllegalStateException),
            // щоб виклик sha256() не зобов'язував усіх клієнтів писати throws
            // Передаємо оригінальний e як cause — стек-трейс не загубиться
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /*
     * verifyHash(input, expectedHash) — перевіряє, чи відповідає рядок очікуваному хешу.
     *
     * Сценарій: у БД ми зберігали SHA-256 від refresh-токена. Прийшов клієнт із "сирим" токеном.
     * Хешуємо отриманий токен і порівнюємо з тим, що в БД.
     *
     * Повертає true, якщо хеші збігаються, false — якщо ні.
     */
    public static boolean verifyHash(String input, String expectedHash) {
        // Захист від NPE для обох параметрів
        if(input == null || expectedHash == null) {
            throw new IllegalArgumentException("Input and expected hash cannot be null");
        }

        // Спочатку хешуємо вхідний рядок (отримуємо HEX-рядок з 64 символів)
        String computedHash = sha256(input);

        // ВАЖЛИВО: НЕ використовуємо звичайний equals() для порівняння хешів!
        //
        // MessageDigest.isEqual() — це "constant-time comparison":
        //   порівнює всі байти незалежно від того, де знайшлось перше розходження.
        //
        // Звичайний equals() виходить одразу при першому відмінному байті,
        // що дозволяє атакуючому виміряти час відповіді та поступово вгадати хеш (timing attack).
        //
        // .getBytes(UTF_8) — конвертуємо обидва HEX-рядки у byte[] для побайтового порівняння
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}
