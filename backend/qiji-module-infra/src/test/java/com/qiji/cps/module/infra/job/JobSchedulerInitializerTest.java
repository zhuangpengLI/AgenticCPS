package com.qiji.cps.module.infra.job;

import com.qiji.cps.module.infra.service.job.JobService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JobSchedulerInitializerTest {

    @Test
    void onApplicationReady_syncsPersistedJobs() throws Exception {
        JobService jobService = mock(JobService.class);
        JobSchedulerInitializer initializer = new JobSchedulerInitializer(jobService);

        initializer.onApplicationReady();

        verify(jobService).syncJob();
    }

}
