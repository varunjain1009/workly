package com.workly.modules.job.dto;

import com.workly.modules.job.AssignmentMode;
import com.workly.modules.job.JobStatus;
import com.workly.modules.job.JobType;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDTO {
    private String id;
    private String title;
    private String description;
    private String requiredSkill;
    private List<String> requiredSkills;
    private LocationDTO location;
    private int searchRadiusKm;
    private long preferredDateTime;
    private JobType jobType;
    private AssignmentMode assignmentMode;
    private Double budget;
    private JobStatus status;
    private boolean toolsRequired;
    private boolean immediate;
    private Double penaltyAmount;
    private String cancellationReason;

    // Promotions
    private String appliedPromoCode;
    private double discountAmount;

    // Assigned job details
    private String completionOtp;
    private String workerMobileNumber;
    private String workerName;
    private String seekerMobileNumber;
}
