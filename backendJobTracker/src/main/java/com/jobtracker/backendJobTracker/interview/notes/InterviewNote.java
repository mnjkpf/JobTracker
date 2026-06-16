package com.jobtracker.backendJobTracker.interview.notes;

import java.time.Instant;
import java.util.UUID;

import com.jobtracker.backendJobTracker.interview.InterviewPrep;
import com.jobtracker.backendJobTracker.interview.notes.enums.NoteType;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "interview_notes")
@Getter
@Setter
public class InterviewNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_prep_id", nullable = false)
    private InterviewPrep interviewPrep;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    private NoteType noteType;

    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    
    @Column(name = "embedding", insertable = false, updatable = false,
            columnDefinition = "vector(1536)")
    private float[] embedding;

    
    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}