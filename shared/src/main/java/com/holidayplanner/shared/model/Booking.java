package com.holidayplanner.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to FamilyMember (managed by IdentityService)
    @Column(nullable = false)
    private UUID familyMemberId;

    // Reference to EventTerm (managed by EventService)
    @Column(nullable = false)
    private UUID eventTermId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime bookedAt;

    @PrePersist
    public void prePersist() {
        this.bookedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BookingStatus.CONFIRMED;
        }
    }
}
