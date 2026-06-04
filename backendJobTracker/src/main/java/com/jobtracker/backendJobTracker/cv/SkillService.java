package com.jobtracker.backendJobTracker.cv;

import java.util.UUID;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.cv.enums.SkillCategory;
import com.jobtracker.backendJobTracker.cv.models.Skill;
import com.jobtracker.backendJobTracker.cv.repo.SkillRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillService {

    
    private final SkillRepository skillRepository;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public Skill findOrCreateSkill(String rawName, SkillCategory category) {
        Skill skill = skillRepository.findByName(rawName.toLowerCase())
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(rawName.toLowerCase());
                    newSkill.setCategory(category);
                    return skillRepository.save(newSkill);
                });
        return skill;
    }

    /**
     * Асинхронно генерує vector-embedding для скіла за його назвою та зберігає
     * у колонку {@code skills.embedding} (pgvector, 1536 dims).
     *
     * <p>Викликати ТІЛЬКИ з іншого біна (напр. {@link CvService}) — при
     * self-invocation у межах цього класу проксі {@code @Async}/{@code @Transactional}
     * не спрацює, і метод виконається синхронно без транзакції.
     *
     * <p>{@code @Transactional} (read-write) свідомо перекриває класовий
     * {@code readOnly = true}, бо метод пише в БД. Запис іде через нативний
     * {@link JdbcTemplate}, оскільки сутність {@link Skill} не мапить колонку
     * embedding.
     */
    @Async // виконати в окремому потоці (з пулу), не блокуючи виклик-ініціатор
    @Transactional // read-write транзакція; перекриває класовий readOnly = true, бо тут є запис
    public void triggerEmbeddingGeneration(UUID skillId) {
        // Дістаємо скіл за id; orElse(null) — щоб не кидати виняток, а тихо обробити відсутність
        Skill skill = skillRepository.findById(skillId).orElse(null);
        // Скіл міг бути видалений між створенням і запуском async-задачі
        if (skill == null) {
            // Лог рівня WARN: ситуація нештатна, але не критична
            log.warn("Skill {} не знайдено — пропускаю генерацію embedding", skillId);
            // Виходимо — генерувати embedding нема для чого
            return;
        }

        try {
            // Звертаємось до моделі (OpenAI через Spring AI): назва скіла → вектор float[1536]
            float[] embedding = embeddingModel.embed(skill.getName());

            // Пишемо вектор сирим SQL, бо сутність Skill не мапить колонку embedding.
            // update(...) повертає к-сть змінених рядків (очікуємо 1)
            int updated = jdbcTemplate.update(
                    // CAST(? AS vector) — текстовий літерал "[...]" каститься у pgvector-тип
                    "UPDATE skills SET embedding = CAST(? AS vector) WHERE id = ?",
                    toVectorLiteral(embedding), // 1-й ? : вектор у вигляді "[v1,v2,...]"
                    skillId);                   // 2-й ? : id скіла для WHERE

            // DEBUG-лог: підтвердження успіху + розмірність вектора й к-сть оновлених рядків
            log.debug("Згенеровано embedding для скіла {} ('{}'): {} dims, оновлено {} рядків",
                    skillId, skill.getName(), embedding.length, updated);
        } catch (Exception e) {
            // Fire-and-forget: помилка одного скіла (напр. збій API) не повинна валити виклик.
            // Ловимо все, логуємо рівнем ERROR разом зі стектрейсом (останній аргумент e)
            log.error("Не вдалося згенерувати embedding для скіла {} ('{}'): {}",
                    skillId, skill.getName(), e.getMessage(), e);
        }
    }

    /** Форматує {@code float[]} у pgvector-літерал виду {@code [v1,v2,...]}. */
    private static String toVectorLiteral(float[] vector) {
        // Заздалегідь резервуємо ємність (~8 символів на число + дужки), щоб уникнути перевиділень
        StringBuilder sb = new StringBuilder(vector.length * 8 + 2);
        sb.append('['); // відкриваюча дужка pgvector-літерала
        // Проходимо всі компоненти вектора по черзі
        for (int i = 0; i < vector.length; i++) {
            // Кома-роздільник перед кожним числом, крім першого (щоб не було провідної коми)
            if (i > 0) {
                sb.append(',');
            }
            // Додаємо число; Float.toString завжди використовує крапку як десятковий роздільник
            // (не залежить від локалі) → pgvector коректно його розпарсить
            sb.append(vector[i]);
        }
        sb.append(']'); // закриваюча дужка
        return sb.toString(); // готовий рядок, напр. "[0.021,-0.044,0.131]"
    }
}
