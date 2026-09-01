package com.atguigu.java.ai.langchain4j.common.retention;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hbti.retention.cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);
    private final RetentionCleanupService service;

    public RetentionCleanupJob(RetentionCleanupService service) {
        this.service = service;
    }

    @Scheduled(
            initialDelayString = "${hbti.retention.cleanup-initial-delay:PT1H}",
            fixedDelayString = "${hbti.retention.cleanup-interval:PT24H}")
    public void purgeExpiredData() {
        RetentionCleanupService.RetentionCleanupResult result = service.purgeExpiredData();
        log.info("retention_cleanup_completed refresh_tokens_deleted={} audit_events_deleted={}",
                result.refreshTokensDeleted(), result.auditEventsDeleted());
    }
}
