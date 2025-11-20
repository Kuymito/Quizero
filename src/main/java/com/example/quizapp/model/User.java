package com.example.quizapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"quizzes", "taughtClasses", "enrolledClasses"}) // Prevent infinite loops
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Use ONLY the ID for equality
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // A teacher can create many quizzes
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Quiz> quizzes;

    // For Teachers: Classes they created
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Classroom> taughtClasses;

    // For Students: Classes they joined
    @ManyToMany(mappedBy = "students", fetch = FetchType.LAZY)
    private Set<Classroom> enrolledClasses;

    public enum Role {
        ROLE_ADMIN,
        ROLE_TEACHER,
        ROLE_STUDENT
    }
}