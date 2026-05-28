package com.holidayplanner.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to the owning organization (managed by OrganizationService)
    @Column(nullable = false)
    private UUID organizationId;

    // Reference to the event owner (managed by IdentityService)
    @Column(nullable = false)
    private UUID eventOwnerId;

    @Column(nullable = false)
    private String shortTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String pictureUrl;

    @Column(nullable = false)
    private String location;

    private String meetingPoint;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private int minimalAge;

    @Column(nullable = false)
    private int maximalAge;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventTerm> terms = new ArrayList<>();
}
