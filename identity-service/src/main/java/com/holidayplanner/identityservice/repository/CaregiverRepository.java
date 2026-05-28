package com.holidayplanner.identityservice.repository;

import com.holidayplanner.shared.model.Caregiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {
    Optional<Caregiver> findByEmail(String email);
    boolean existsByEmail(String email);
}
