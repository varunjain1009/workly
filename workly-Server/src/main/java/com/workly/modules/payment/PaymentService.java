package com.workly.modules.payment;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobService;
import com.workly.modules.job.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final JobService jobService;

    public PaymentTransaction createPaymentIntent(String jobId, String seekerMobile) {
        log.debug("PaymentService: [ENTER] createPaymentIntent - jobId: {}, seeker: {}", jobId, seekerMobile);
        Job job = jobService.getJobById(jobId);

        if (!job.getSeekerMobileNumber().equals(seekerMobile)) {
            throw WorklyException.forbidden("You do not own this job");
        }
        if (job.getBudget() <= 0) {
            throw WorklyException.badRequest("Job budget must be greater than zero to create a payment intent");
        }
        if (job.getStatus() != JobStatus.CREATED && job.getStatus() != JobStatus.BROADCASTED && job.getStatus() != JobStatus.ASSIGNED && job.getStatus() != JobStatus.SCHEDULED) {
            throw WorklyException.badRequest("Job is not in a valid state for payment intent creation");
        }

        if (paymentRepository.findByJobId(jobId).isPresent()) {
            throw WorklyException.badRequest("Payment Intent already exists for this job");
        }

        PaymentTransaction tx = new PaymentTransaction();
        tx.setJobId(jobId);
        tx.setSeekerMobileNumber(seekerMobile);
        tx.setWorkerMobileNumber(job.getWorkerMobileNumber());

        double gross = job.getBudget();
        double commission = gross * 0.10; // 10% commission

        tx.setGrossAmount(gross);
        tx.setCommissionAmount(commission);
        tx.setNetProviderAmount(gross - commission);
        tx.setStatus(PaymentTransaction.TransactionStatus.ESCROW_LOCKED);
        tx.setPaymentIntentId("pi_" + UUID.randomUUID().toString()); // Mock Stripe ID

        PaymentTransaction saved = paymentRepository.save(tx);
        log.info("PaymentService: Escrow locked pi {} for Job {}", saved.getPaymentIntentId(), jobId);
        return saved;
    }

    public void completePayment(String jobId) {
        paymentRepository.findByJobId(jobId).ifPresent(tx -> {
            tx.setStatus(PaymentTransaction.TransactionStatus.COMPLETED);
            paymentRepository.save(tx);
            log.info("PaymentService: Payment for job {} marked as COMPLETED and moved to worker's ledger", jobId);
        });
    }

    public List<PaymentTransaction> getProviderLedger(String workerMobile) {
        return paymentRepository.findByWorkerMobileNumberAndStatus(workerMobile, PaymentTransaction.TransactionStatus.COMPLETED);
    }
}
