package com.workly.modules.payment;

import com.workly.core.WorklyException;
import com.workly.modules.job.Job;
import com.workly.modules.job.JobService;
import com.workly.modules.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private JobService jobService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        paymentService = new PaymentService(paymentRepository, jobService);
        ReflectionTestUtils.setField(paymentService, "paymentsEnabled", true);
    }

    private Job job(String seeker, String worker, double budget, JobStatus status) {
        Job j = new Job();
        j.setSeekerMobileNumber(seeker);
        j.setWorkerMobileNumber(worker);
        j.setBudget(budget);
        j.setStatus(status);
        return j;
    }

    @Test
    void createPaymentIntent_success() {
        Job j = job("s1", "w1", 100.0, JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);
        when(paymentRepository.findByJobId("j1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction tx = paymentService.createPaymentIntent("j1", "s1");

        assertEquals(PaymentTransaction.TransactionStatus.ESCROW_LOCKED, tx.getStatus());
        assertEquals("j1", tx.getJobId());
        assertTrue(tx.getPaymentIntentId().startsWith("pi_"));
        verify(paymentRepository).save(any());
    }

    @Test
    void createPaymentIntent_paymentsDisabled_bypass() {
        ReflectionTestUtils.setField(paymentService, "paymentsEnabled", false);
        Job j = job("s1", "w1", 100.0, JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);
        when(paymentRepository.findByJobId("j1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransaction tx = paymentService.createPaymentIntent("j1", "s1");

        assertEquals(PaymentTransaction.TransactionStatus.COMPLETED, tx.getStatus());
        assertEquals("BYPASSED", tx.getPaymentIntentId());
    }

    @Test
    void createPaymentIntent_forbidden_throws() {
        Job j = job("s1", "w1", 100.0, JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);

        assertThrows(WorklyException.class, () -> paymentService.createPaymentIntent("j1", "other"));
    }

    @Test
    void createPaymentIntent_zeroBudget_throws() {
        Job j = job("s1", "w1", 0.0, JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);

        assertThrows(WorklyException.class, () -> paymentService.createPaymentIntent("j1", "s1"));
    }

    @Test
    void createPaymentIntent_invalidStatus_throws() {
        Job j = job("s1", "w1", 100.0, JobStatus.COMPLETED);
        when(jobService.getJobById("j1")).thenReturn(j);

        assertThrows(WorklyException.class, () -> paymentService.createPaymentIntent("j1", "s1"));
    }

    @Test
    void createPaymentIntent_duplicate_throws() {
        Job j = job("s1", "w1", 100.0, JobStatus.ASSIGNED);
        when(jobService.getJobById("j1")).thenReturn(j);
        when(paymentRepository.findByJobId("j1")).thenReturn(Optional.of(new PaymentTransaction()));

        assertThrows(WorklyException.class, () -> paymentService.createPaymentIntent("j1", "s1"));
    }

    @Test
    void completePayment_found_updatesStatus() {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus(PaymentTransaction.TransactionStatus.ESCROW_LOCKED);
        when(paymentRepository.findByJobId("j1")).thenReturn(Optional.of(tx));
        when(paymentRepository.save(tx)).thenReturn(tx);

        paymentService.completePayment("j1");

        assertEquals(PaymentTransaction.TransactionStatus.COMPLETED, tx.getStatus());
        verify(paymentRepository).save(tx);
    }

    @Test
    void completePayment_notFound_doesNothing() {
        when(paymentRepository.findByJobId("j1")).thenReturn(Optional.empty());
        paymentService.completePayment("j1");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getProviderLedger_returnsList() {
        List<PaymentTransaction> txs = List.of(new PaymentTransaction());
        when(paymentRepository.findByWorkerMobileNumberAndStatus("w1", PaymentTransaction.TransactionStatus.COMPLETED))
                .thenReturn(txs);

        assertEquals(1, paymentService.getProviderLedger("w1").size());
    }
}
