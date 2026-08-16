package com.job.scheduler.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.job.scheduler.entity.DeadLetterEntry;
import com.job.scheduler.entity.Job;
import com.job.scheduler.entity.JobExecution;
import com.job.scheduler.entity.JobLog;
import com.job.scheduler.enums.JobStatus;
import com.job.scheduler.handler.JobHandler;
import com.job.scheduler.repository.DeadLetterEntryRepository;
import com.job.scheduler.repository.JobExecutionRepository;
import com.job.scheduler.repository.JobLogRepository;
import com.job.scheduler.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
class JobExecutorTest {
    @Mock JobRepository jobs;
    @Mock JobExecutionRepository executions;
    @Mock JobLogRepository logs;
    @Mock DeadLetterEntryRepository deadLetters;

    private JobExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    void successfulHandlerCompletesJobAndExecution() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        executor = new JobExecutor(jobs, executions, logs, deadLetters, Map.of("email", handler));
        Job job = job("email", 3, 0);
        when(executions.save(any(JobExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        executor.submit(job, "worker-1");

        verify(handler, timeout(1000)).handle("payload");
        verify(jobs, timeout(1000).atLeast(2)).save(job);
        verify(executions, timeout(1000).times(2)).save(argThat(execution ->
                execution.getStatus() == JobStatus.COMPLETED && execution.getCompletedAt() != null));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
        assertEquals("worker-1", job.getWorkerId());
    }

    @Test
    void failedFinalAttemptMovesJobToDeadLetterWithSnapshot() throws Exception {
        JobHandler handler = mock(JobHandler.class);
        doThrow(new IllegalStateException("remote service unavailable")).when(handler).handle("payload");
        executor = new JobExecutor(jobs, executions, logs, deadLetters, Map.of("email", handler));
        Job job = job("email", 2, 1);
        when(executions.save(any(JobExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        executor.submit(job, "worker-1");

        ArgumentCaptor<DeadLetterEntry> entry = ArgumentCaptor.forClass(DeadLetterEntry.class);
        verify(deadLetters, timeout(1000)).save(entry.capture());
        assertEquals(JobStatus.DEAD_LETTER, job.getStatus());
        assertEquals(2, job.getRetryCount());
        assertEquals("remote service unavailable", job.getLastError());
        assertEquals(job, entry.getValue().getJob());
        assertEquals("payload", entry.getValue().getPayloadSnapshot());
        verify(executions, timeout(1000).times(2)).save(argThat(execution ->
                execution.getStatus() == JobStatus.FAILED && "remote service unavailable".equals(execution.getErrorMessage())));
    }

    @Test
    void missingHandlerFailsPermanentlyWithoutInvokingAHandler() {
        executor = new JobExecutor(jobs, executions, logs, deadLetters, Map.of());
        Job job = job("unknown", 3, 0);
        when(executions.save(any(JobExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        executor.submit(job, "worker-1");

        verify(deadLetters, timeout(1000)).save(argThat(entry ->
                entry.getReason().equals("No handler registered for type: unknown")));
        assertEquals(JobStatus.DEAD_LETTER, job.getStatus());
        assertEquals(0, job.getRetryCount());
    }

    private Job job(String type, int maxRetries, int retryCount) {
        Job job = new Job();
        job.setType(type);
        job.setPayload("payload");
        job.setMaxRetries(maxRetries);
        job.setRetryCount(retryCount);
        job.setCreatedAt(Instant.now());
        return job;
    }
}
