package com.holidayplanner.eventservice.repository;

import com.holidayplanner.shared.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByOrganizationId(UUID organizationId);

    List<Event> findByEventOwnerId(UUID eventOwnerId);
}
