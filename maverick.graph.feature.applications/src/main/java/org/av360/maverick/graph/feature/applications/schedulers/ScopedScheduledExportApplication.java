package org.av360.maverick.graph.feature.applications.schedulers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationJobScheduledEvent;
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

    @PostConstruct
    public void initializeScheduledJobs() {
        applicationsService.listApplications(new AdminToken())
                .doOnNext(application -> {
                    if (!application.configuration().containsKey(CONFIG_KEY_EXPORT_FREQUENCY)) return;

                    Runnable task = () -> {
                        JobScheduledEvent event = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), application);
                        eventPublisher.publishEvent(event);
                    };

                    ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_EXPORT_FREQUENCY).toString()));
                    scheduledTasks.put(application.label(), scheduledFuture);
                }).subscribe();
    }


    @EventListener
    public void handleApplicationCreated(ApplicationCreatedEvent event) {
//        applicationsService.getApplication(event.getApplication(), new AdminToken())
//                .subscribe(newApplication -> {
                    if (!event.getApplication().configuration().containsKey(CONFIG_KEY_EXPORT_FREQUENCY)) return;

                    Runnable task = () -> {
                        JobScheduledEvent jobEvent = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), event.getApplication());
                        eventPublisher.publishEvent(jobEvent);
                    };
                    ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(event.getApplication().configuration().get(CONFIG_KEY_EXPORT_FREQUENCY).toString()));
                    scheduledTasks.put(event.getApplication().label(), scheduledFuture);
//                });
    }

    @EventListener
    public void handleApplicationDeleted(ApplicationDeletedEvent event) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.get(event.getApplicationLabel());
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledTasks.remove(event.getApplicationLabel());
        }
    }
}
