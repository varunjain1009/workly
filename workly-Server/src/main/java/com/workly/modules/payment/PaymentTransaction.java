package com.workly.modules.payment;

import com.workly.core.MongoBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "payment_transactions")
public class PaymentTransaction extends MongoBaseEntity {
    @Id
    private String id;
    private String jobId;
    private String seekerMobileNumber;
    private String workerMobileNumber;
    
    private double grossAmount;
    private double commissionAmount; // E.g., 10%
    private double netProviderAmount;
    
    private TransactionStatus status;
    private String paymentIntentId; // Mock Stripe Intent ID

    public enum TransactionStatus {
        ESCROW_LOCKED,
        COMPLETED, // Funds cleared and added to ledger
        REFUNDED,
        DISPUTED
    }
}
