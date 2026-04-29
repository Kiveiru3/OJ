package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class DiscussionAuditDTO {
    /**
     * 1-approved, 2-rejected
     */
    @NotNull(message = "auditStatus is required")
    private Integer auditStatus;

    @Size(max = 300, message = "auditRemark must not exceed 300 characters")
    private String auditRemark;
}

