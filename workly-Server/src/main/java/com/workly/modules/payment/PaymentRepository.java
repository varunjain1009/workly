package com.workly.modules.payment;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<PaymentTransaction, String> {
    Optional<PaymentTransaction> findByJobId(String jobId);
    List<PaymentTransaction> findByWorkerMobileNumberAndStatus(String workerMobile, PaymentTransaction.TransactionStatus status);
}
