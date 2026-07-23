package com.jobtracker.backendJobTracker.interview;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.enums.QuestionCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "interview_questions")
public class InterviewQuestion {
 
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_prep_id", nullable = false)
    private InterviewPrep interviewPrep;
 
    // ДОДАНО: nullable=false
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionCategory category;
 
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
 
    // ВИПРАВЛЕНО: NULLABLE. behavioral і questions-to-ask не мають suggested answer.
    // Прибрано nullable=false.
    @Column(name = "suggested_answer", columnDefinition = "TEXT")
    private String suggestedAnswer;
 
    
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
 
    
 
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

