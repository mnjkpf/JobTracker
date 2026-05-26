package com.jobtracker.backendJobTracker.company;

import java.util.UUID;

import com.jobtracker.backendJobTracker.user.User;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "companies",
    // ДОДАНО: composite unique. Юзер не може мати дві компанії "Allegro",
    // але два юзери — можуть мати "Allegro" кожен. Глобальний unique на name зломав би.
    uniqueConstraints = @UniqueConstraint(
        name = "uk_companies_user_name",
        columnNames = {"user_id", "name"}
    )
)
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    // Все nullable — юзер може створити компанію знаючи лише назву.
    private String website;
    private String industry;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private CompanySize size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}