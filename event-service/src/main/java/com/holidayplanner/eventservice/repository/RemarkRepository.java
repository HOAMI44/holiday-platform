package com.holidayplanner.eventservice.repository;

import com.holidayplanner.shared.model.Remark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RemarkRepository extends JpaRepository<Remark, UUID> {

    List<Remark> findByEventTermId(UUID eventTermId);

    List<Remark> findByFamilyMemberId(UUID familyMemberId);
}
