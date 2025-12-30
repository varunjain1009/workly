package com.workly.modules.verification;

import com.workly.core.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "job_completions")
@EqualsAndHashCode(callSuper = true)
public class JobCompletion extends JpaBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", unique = true, nullable = false)
    private String jobId;

    @Column(name = "worker_mobile", nullable = false)
    private String workerMobile;

    @Column(name = "seeker_mobile", nullable = false)
    private String seekerMobile;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "verification_otp")
    private String verificationOtp;

    @Column(name = "is_verified")
    private boolean verified = false;
}
