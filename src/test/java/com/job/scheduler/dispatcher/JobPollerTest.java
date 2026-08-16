package com.job.scheduler.dispatcher;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.job.scheduler.entity.Job;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.service.WorkerService;

@ExtendWith(MockitoExtension.class)
class JobPollerTest {
    @Mock JobRepository jobs;
    @Mock JobExecutor executor;
    @Mock WorkerService workers;

    @Test
    void pollClaimsAndSubmitsEveryClaimedJob() {
        Job first = new Job();
        Job second = new Job();
        when(jobs.claimNextJobIds(anyString(), anyInt())).thenReturn(List.of(3L, 7L));
        when(jobs.findByIdIn(List.of(3L, 7L))).thenReturn(List.of(first, second));

        new JobPoller(jobs, executor, workers).pollAndClaim();

        verify(jobs).findByIdIn(List.of(3L, 7L));
        verify(executor).submit(eq(first), anyString());
        verify(executor).submit(eq(second), anyString());
    }

    @Test
    void pollDoesNotClaimWorkWhileExecutorIsDraining() {
        when(executor.isShuttingDown()).thenReturn(true);

        new JobPoller(jobs, executor, workers).pollAndClaim();

        verify(jobs, never()).claimNextJobIds(anyString(), anyInt());
    }

    @Test
    void heartbeatReportsCurrentExecutorLoad() {
        when(executor.getActiveJobCount()).thenReturn(4);

        new JobPoller(jobs, executor, workers).heartbeat();

        verify(workers).heartbeat(anyString(), org.mockito.ArgumentMatchers.eq(4));
    }
}
