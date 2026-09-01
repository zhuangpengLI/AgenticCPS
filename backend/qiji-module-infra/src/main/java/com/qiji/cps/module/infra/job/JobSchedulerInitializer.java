package com.qiji.cps.module.infra.job;

import com.qiji.cps.module.infra.service.job.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Restores persisted job definitions into Quartz after the application starts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JobSchedulerInitializer {

    private final JobService jobService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            jobService.syncJob();
            log.info("[onApplicationReady][已将数据库中的定时任务恢复到 Quartz]");
        } catch (SchedulerException e) {
            log.error("[onApplicationReady][恢复数据库中的定时任务到 Quartz 失败]", e);
        }
    }

}
