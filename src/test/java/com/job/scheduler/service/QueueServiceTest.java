package com.job.scheduler.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.job.scheduler.dto.QueueRequest;
import com.job.scheduler.entity.Project;
import com.job.scheduler.entity.Queue;
import com.job.scheduler.entity.User;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.ProjectRepository;
import com.job.scheduler.repository.QueueRepository;
import com.job.scheduler.enums.RetryMethods;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {
    @Mock QueueRepository queues;
    @Mock ProjectRepository projects;
    @Mock JobRepository jobs;
    private QueueService service;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new QueueService(queues, projects, jobs);
        User owner = new User(); owner.setId(10L);
        project = new Project(); project.setId(3L); project.setOwner(owner);
    }

    @Test
    void createAppliesRequestAndIncludesZeroCounts() {
        QueueRequest request = new QueueRequest();
        request.setName("critical"); request.setPriority(9); request.setConcurrencyLimit(2);
        request.setDefaultMaxRetries(4); request.setDefaultRetryStrategy(RetryMethods.LINEAR);
        when(projects.findById(3L)).thenReturn(Optional.of(project));
        when(queues.save(any(Queue.class))).thenAnswer(inv -> { Queue q = inv.getArgument(0); q.setId(8L); return q; });
        when(jobs.countByStatusForQueue(8L)).thenReturn(java.util.List.of());

        var response = service.create(10L, 3L, request);

        assertEquals("critical", response.getName());
        assertEquals(9, response.getPriority());
        assertEquals(4, response.getDefaultMaxRetries());
        assertTrue(response.getJobCountsByStatus().containsKey("QUEUED"));
    }

    @Test
    void nonOwnerCannotReadQueue() {
        Queue queue = new Queue(); queue.setId(8L); queue.setProject(project);
        when(queues.findById(8L)).thenReturn(Optional.of(queue));

        assertThrows(AccessDeniedException.class, () -> service.get(99L, 8L));
    }
}
