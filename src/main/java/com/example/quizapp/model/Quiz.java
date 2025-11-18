package com.example.quizapp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

// TODO: Add other entities like Question, Answer, QuizAttempt
// Updated TODO: Entities are now added.

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "quizzes")
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    private String subject;

    // Many quizzes can be created by one teacher
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    // TODO: Add relationship to Questions
    // @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    // private Set<Question> questions;

    // Lazy-loaded collection - MUST be excluded from hashCode/equals
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Question> questions = new HashSet<>();

    // Lazy-loaded collection - MUST be excluded from hashCode/equals
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private Set<QuizAttempt> quizAttempts;
}