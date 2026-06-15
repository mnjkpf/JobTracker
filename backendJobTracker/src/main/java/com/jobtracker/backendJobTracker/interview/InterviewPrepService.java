package com.jobtracker.backendJobTracker.interview;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.Application;
import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.cv.models.MasterCv;
import com.jobtracker.backendJobTracker.cv.repo.MasterCvRepository;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.interview.dto.InterviewPrepResponse;
import com.jobtracker.backendJobTracker.interview.dto.InterviewQuestionResponse;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedPrepGuide;
import com.jobtracker.backendJobTracker.interview.dto.parse.ParsedQuestion;
import com.jobtracker.backendJobTracker.interview.enums.InterviewPrepStatus;
import com.jobtracker.backendJobTracker.interview.enums.QuestionCategory;
import com.jobtracker.backendJobTracker.interview.repos.InterviewPrepRepository;
import com.jobtracker.backendJobTracker.interview.repos.InterviewQuestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewPrepService {
 
    private final InterviewPrepRepository interviewPrepRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final ApplicationRepository applicationRepository;
    private final MasterCvRepository masterCvRepository;
    private final InterviewPrepMapper mapper;
    private final InterviewPrepGenerator generator;
    
    @Transactional   // ДОДАНО: клас readOnly, метод пише — потрібен override
    public void createIfNotExists(UUID applicationId) {
        if (interviewPrepRepository.existsByApplicationId(applicationId)) {
            return;
        }
 
        Application app = applicationRepository.findById(applicationId)
                // ВИПРАВЛЕНО: типізована exception замість дефолтної NoSuchElementException
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));
 
        InterviewPrep prep = new InterviewPrep();
        prep.setApplication(app);
        prep.setStatus(InterviewPrepStatus.DRAFT);
        interviewPrepRepository.save(prep);
    }
 
    
 
    @Transactional   // ДОДАНО: був відсутній — клас readOnly
    public InterviewPrepResponse generate(UUID userId, UUID applicationId) {
        
        Application app = fetchOwnedApplication(userId, applicationId);
        ensureJobHasDescription(app);
 
        
        MasterCv master = masterCvRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Upload your CV first — interview prep needs context."));
 
        
        createIfNotExists(applicationId);
 
        // ВИПРАВЛЕНО: orElseThrow з Supplier (lambda), не instance
        InterviewPrep prep = interviewPrepRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview prep not found for application: " + applicationId));
 
        
        ParsedPrepGuide parsed = generator.generate(app, master);
 
        
        interviewQuestionRepository.deleteByInterviewPrepId(prep.getId());
        interviewQuestionRepository.flush();
 
        
        attachQuestions(prep, parsed);
 
        
        prep.setStatus(InterviewPrepStatus.GENERATED);
        prep.setPromptVersion(InterviewPrepPrompt.VERSION);
        interviewPrepRepository.save(prep);
 
        
        return loadFullResponse(prep);
    }
 
    
 
    // ДОДАНО: бракувало повністю
    public InterviewPrepResponse getByApplication(UUID userId, UUID applicationId) {
        // Tenant safety: fetch Application першим (через user-FK)
        fetchOwnedApplication(userId, applicationId);
 
        InterviewPrep prep = interviewPrepRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview prep not found for application: " + applicationId));
        return loadFullResponse(prep);
    }
 
    // ДОДАНО: бракувало повністю
    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        fetchOwnedApplication(userId, applicationId);
 
        InterviewPrep prep = interviewPrepRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interview prep not found for application: " + applicationId));
        interviewPrepRepository.delete(prep);   // CASCADE забере questions з БД
    }
 
    
 
    
    private InterviewPrepResponse loadFullResponse(InterviewPrep prep) {
        InterviewPrepResponse response = mapper.toInterviewPrepResponse(prep);
 
        List<InterviewQuestionResponse> technical = new ArrayList<>();
        List<InterviewQuestionResponse> behavioral = new ArrayList<>();
        List<InterviewQuestionResponse> questionsToAsk = new ArrayList<>();
 
        for (InterviewQuestion q : interviewQuestionRepository
                .findByInterviewPrepIdOrderByDisplayOrderAsc(prep.getId())) {
            InterviewQuestionResponse qr = mapper.toInterviewQuestionResponse(q);
            switch (q.getCategory()) {
                case TECHNICAL      -> technical.add(qr);
                case BEHAVIORAL     -> behavioral.add(qr);
                case QUESTION_TO_ASK -> questionsToAsk.add(qr);
            }
        }
 
        response.setTechnicalQuestions(technical);
        response.setBehavioralQuestions(behavioral);
        response.setQuestionsToAsk(questionsToAsk);
        return response;
    }
 
    private Application fetchOwnedApplication(UUID userId, UUID applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));
    }
 
    private void ensureJobHasDescription(Application app) {
        if (app.getDescription() == null || app.getDescription().isBlank()) {
            throw new BusinessRuleException(
                    "Application has no description. Add job posting details first.");
        }
    }
 
    
    private void attachQuestions(InterviewPrep prep, ParsedPrepGuide parsed) {
        int order = 1;
        order = saveQuestions(prep, parsed.getTechnical(), QuestionCategory.TECHNICAL, order);
        order = saveQuestions(prep, parsed.getBehavioral(), QuestionCategory.BEHAVIORAL, order);
        saveQuestions(prep, parsed.getQuestionsToAsk(), QuestionCategory.QUESTION_TO_ASK, order);
    }
 
    /** Зберігає питання однієї категорії, повертає наступний displayOrder. */
    private int saveQuestions(InterviewPrep prep, List<ParsedQuestion> items,
                            QuestionCategory category, int startOrder) {
        if (items == null) return startOrder;
        int order = startOrder;
        for (ParsedQuestion pq : items) {
            if (pq == null || pq.getQuestion() == null || pq.getQuestion().isBlank()) continue;
 
            InterviewQuestion q = new InterviewQuestion();
            q.setInterviewPrep(prep);
            q.setCategory(category);
            q.setQuestion(pq.getQuestion());
            q.setSuggestedAnswer(pq.getSuggestedAnswer());    // правильно — з того ж елемента
            q.setDisplayOrder(order++);
            interviewQuestionRepository.save(q);
        }
        return order;
    }
}
