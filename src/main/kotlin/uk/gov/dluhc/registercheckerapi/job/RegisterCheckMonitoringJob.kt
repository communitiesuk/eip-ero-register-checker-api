package uk.gov.dluhc.registercheckerapi.job

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.registercheckerapi.service.RegisterCheckMonitoringService

@Component
@ConditionalOnProperty("jobs.register-check-monitoring.enabled", havingValue = "true")
class RegisterCheckMonitoringJob(
    private val registerCheckMonitoringService: RegisterCheckMonitoringService
) {
    @Scheduled(cron = "\${jobs.register-check-monitoring.cron}")
    @SchedulerLock(name = "\${jobs.register-check-monitoring.name}")
    fun monitorPendingRegisterChecks() {
        registerCheckMonitoringService.monitorPendingRegisterChecks()
    }
}
