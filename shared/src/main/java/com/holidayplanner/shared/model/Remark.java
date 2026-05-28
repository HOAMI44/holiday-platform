package com.holidayplanner.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "remarks")
@Getter
@Setter
@NoArgsConstructor
public class Remark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to the event term this remark belongs to
    @Column(nullable = false)
    private UUID eventTermId;

    // Reference to the family member (participant) this remark is about
    @Column(nullable = false)
    private UUID familyMemberId;

    // Reference to the event owner who wrote the remark
    @Column(nullable = false)
    private UUID eventOwnerId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
