package com.job.scheduler.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.job.scheduler.entity.Job;
import com.job.scheduler.enums.JobStatus;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.service.WorkerService;

@ExtendWith(MockitoExtension.class)
class JobMaintenanceTest {
    @Mock JobRepository jobs;
    @Mock WorkerService workers;

    @Test
    void promoterPassesCurrentTimeToRepository() {
        new JobPromoter(jobs).promoteReadyJobs();

        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        verify(jobs).promoteScheduledJobsToQueued(now.capture());
        assertFalse(now.getValue().isAfter(Instant.now()));
    }

    @Test
    void reclaimerResetsRunningAndClaimedJobsAndMarksStaleWorkersStopped() {
        Job running = stuckJob(JobStatus.RUNNING, "worker-a");
        Job claimed = stuckJob(JobStatus.CLAIMED, "worker-b");
        when(jobs.findStuckJobs(any(Instant.class))).thenReturn(List.of(running));
        when(jobs.findStuckClaims(any(Instant.class))).thenReturn(List.of(claimed));

        new StaleJobReclaimer(jobs, workers).reclaimStuckJobs();

        for (Job job : List.of(running, claimed)) {
            assertEquals(JobStatus.QUEUED, job.getStatus());
            assertNull(job.getWorkerId());
            assertNull(job.getClaimedAt());
            assertNull(job.getStartedAt());
            verify(jobs).save(job);
        }
        verify(workers).markStaleWorkersStopped(any(Instant.class));
    }

    private Job stuckJob(JobStatus status, String workerId) {
        Job job = new Job();
        job.setStatus(status);
        job.setWorkerId(workerId);
        job.setClaimedAt(Instant.now().minusSeconds(60));
        job.setStartedAt(Instant.now().minusSeconds(60));
        return job;
    }
}
