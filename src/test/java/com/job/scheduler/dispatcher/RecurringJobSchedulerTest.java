package com.job.scheduler.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.job.scheduler.entity.Queue;
import com.job.scheduler.entity.RecurringJobDefinition;
import com.job.scheduler.enums.JobStatus;
import com.job.scheduler.enums.RetryMethods;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.RecurringJobDefinitionRepository;

@ExtendWith(MockitoExtension.class)
class RecurringJobSchedulerTest {
    @Mock RecurringJobDefinitionRepository definitions;
    @Mock JobRepository jobs;

    @Test
    void dueActiveDefinitionSpawnsQueuedJobAndAdvancesSchedule() {
        Queue queue = new Queue();
        queue.setPaused(false);
        RecurringJobDefinition definition = definition(queue);
        when(definitions.findByEnabledTrueAndNextRunAtLessThanEqual(any(Instant.class)))
                .thenReturn(List.of(definition));

        new RecurringJobScheduler(definitions, jobs).spawnDueJobs();

        ArgumentCaptor<Job> spawned = ArgumentCaptor.forClass(Job.class);
        verify(jobs).save(spawned.capture());
        Job job = spawned.getValue();
        assertEquals(queue, job.getQueue());
        assertEquals("report", job.getType());
        assertEquals("{\"scope\":\"daily\"}", job.getPayload());
        assertEquals(JobStatus.QUEUED, job.getStatus());
        assertNull(job.getScheduledAt());
        assertEquals(RetryMethods.LINEAR, job.getRetryStrategy());
        assertTrue(definition.getLastRunAt() != null);
        assertTrue(definition.getNextRunAt().isAfter(definition.getLastRunAt()));
        verify(definitions).save(definition);
    }

    @Test
    void pausedQueueAdvancesScheduleWithoutSpawningJob() {
        Queue queue = new Queue();
        queue.setPaused(true);
        RecurringJobDefinition definition = definition(queue);
        when(definitions.findByEnabledTrueAndNextRunAtLessThanEqual(any(Instant.class)))
                .thenReturn(List.of(definition));

        new RecurringJobScheduler(definitions, jobs).spawnDueJobs();

        verify(jobs, never()).save(any());
        verify(definitions).save(definition);
        assertNull(definition.getLastRunAt());
        assertTrue(definition.getNextRunAt().isAfter(Instant.now()));
    }

    private RecurringJobDefinition definition(Queue queue) {
        RecurringJobDefinition definition = new RecurringJobDefinition();
        definition.setId(12L);
        definition.setName("daily report");
        definition.setQueue(queue);
        definition.setType("report");
        definition.setPayload("{\"scope\":\"daily\"}");
        definition.setCronExpression("0 0 6 * * *");
        definition.setPriority(4);
        definition.setMaxRetries(2);
        definition.setRetryStrategy(RetryMethods.LINEAR);
        definition.setNextRunAt(Instant.now().minusSeconds(10));
        return definition;
    }
}
