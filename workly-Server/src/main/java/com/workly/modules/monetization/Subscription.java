package com.workly.modules.monetization;

import com.workly.core.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "subscriptions")
@EqualsAndHashCode(callSuper = true)
public class Subscription extends JpaBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_mobile", nullable = false)
    private String userMobile;

    @Column(name = "plan_type")
    private String planType;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private boolean active = true;
}
