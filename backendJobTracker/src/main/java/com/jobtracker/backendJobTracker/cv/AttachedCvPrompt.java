package com.jobtracker.backendJobTracker.cv;

/**
 * Секція промпту для CV, прикріпленого безпосередньо до заявки.
 * <p>
 * Прикріплене CV — це та версія, яку юзер реально відправив у цю компанію, тому
 * для LLM вона авторитетніша за master CV: питання на співбесіді рекрутер буде
 * ставити по ній, і cover letter має бути узгоджений саме з нею.
 * <p>
 * Спільна для трьох генераторів (cover letter / tailored CV / interview prep),
 * щоб формулювання правила не розповзлося трьома копіями.
 */
public final class AttachedCvPrompt {

    private AttachedCvPrompt() {
    }

    /** Готовий блок промпту, або "" якщо до заявки нічого не прикріплено. */
    public static String section(String attachedCvText) {
        if (attachedCvText == null || attachedCvText.isBlank()) {
            return "";
        }
        return """

                === CV ATTACHED TO THIS APPLICATION (AUTHORITATIVE) ===
                The candidate attached this CV specifically to this application — it is the
                version the employer actually received. Treat it as the authoritative source
                of the candidate's experience, skills and wording: where it conflicts with the
                master CV above, follow this one. The master CV is background context only.
                Never introduce experience or skills that appear in neither document.

                """ + attachedCvText.strip() + "\n";
    }
}
