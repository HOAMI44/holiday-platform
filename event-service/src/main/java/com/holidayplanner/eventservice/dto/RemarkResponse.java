package com.holidayplanner.eventservice.dto;

import com.holidayplanner.shared.model.Remark;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RemarkResponse {
    private UUID id;
    private UUID eventTermId;
    private UUID familyMemberId;
    private UUID eventOwnerId;
    private String description;
    private LocalDateTime createdAt;

    public static RemarkResponse from(Remark remark) {
        RemarkResponse r = new RemarkResponse();
        r.id = remark.getId();
        r.eventTermId = remark.getEventTermId();
        r.familyMemberId = remark.getFamilyMemberId();
        r.eventOwnerId = remark.getEventOwnerId();
        r.description = remark.getDescription();
        r.createdAt = remark.getCreatedAt();
        return r;
    }
}
