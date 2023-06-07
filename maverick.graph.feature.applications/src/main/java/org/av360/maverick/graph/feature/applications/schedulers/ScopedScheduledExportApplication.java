package org.av360.maverick.graph.feature.applications.schedulers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.exportApplication.enabled", havingValue = "true")
public class ScopedScheduledExportApplication {
    public static final String CONFIG_KEY_EXPORT_FREQUENCY = "export_frequency";
    public static final String CONFIG_KEY_EXPORT_S3_HOST = "export_s3_host";
    public static final String CONFIG_KEY_EXPORT_S3_BUCKET = "export_s3_bucket";

    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationsService applicationsService;

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ScopedScheduledExportApplication(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService, TaskScheduler taskScheduler) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
        this.taskScheduler = taskScheduler;
    }

    private void _scheduleRunnableTask(Application application) {
        if (!application.configuration().containsKey(CONFIG_KEY_EXPORT_FREQUENCY)) return;

        Runnable task = () -> {
            JobScheduledEvent event = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), application);
            eventPublisher.publishEvent(event);
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_EXPORT_FREQUENCY).toString()));
        scheduledTasks.put(application.label(), scheduledFuture);
    }

    private void _deleteScheduledTask(String applicationLabel, boolean mayInterruptIfRunning) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.get(applicationLabel);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(mayInterruptIfRunning);
            scheduledTasks.remove(applicationLabel);
        }
    }

    @PostConstruct
    public void initializeScheduledJobs() {
        applicationsService.listApplications(new AdminToken()).doOnNext(this::_scheduleRunnableTask).subscribe();
    }


    @EventListener
    public void handleApplicationCreated(ApplicationCreatedEvent event) {
//        applicationsService.getApplication(event.getApplication(), new AdminToken())
//                .subscribe(newApplication -> {
                        _scheduleRunnableTask(event.getApplication());
//                });
    }

    @EventListener
    public void handleApplicationUpdated(ApplicationUpdatedEvent event) {
        _deleteScheduledTask(event.getApplication().label(), false);
        _scheduleRunnableTask(event.getApplication());
    }



    @EventListener
    public void handleApplicationDeleted(ApplicationDeletedEvent event) {
        _deleteScheduledTask(event.getApplicationLabel(), true);
    }
}
