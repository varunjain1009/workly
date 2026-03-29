package com.workly.modules.promotion;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "promotions")
public class Promotion extends MongoBaseEntity {
    @Id
    private String id;
    private String code;
    private double discountPercentage; // E.g., 20.0 for 20%
    private double maxDiscountAmount;  // E.g., cap at 50$
    private LocalDateTime expirationDate;
    private boolean active;
}
