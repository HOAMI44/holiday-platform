package com.holidayplanner.organizationservice.repository;

import com.holidayplanner.shared.model.Sponsor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SponsorRepository extends JpaRepository<Sponsor, UUID> {
    List<Sponsor> findByOrganization_Id(UUID organizationId);
}
