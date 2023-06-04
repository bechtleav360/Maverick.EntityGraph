package org.av360.maverick.graph.feature.applications.schedulers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

@Component
@Slf4j(topic = "graph.jobs.exports")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.exportApplication.enabled", havingValue = "true")
public class ScopedScheduledExportApplication implements ApplicationListener<ApplicationCreatedEvent> {
    public static final String CONFIG_KEY_EXPORT_FREQUENCY = "export_frequency";
    public static final String CONFIG_KEY_EXPORT_S3_HOST = "export_s3_host";

    public static final String CONFIG_KEY_EXPORT_S3_BUCKET = "export_s3_bucket";

    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationsService applicationsService;

    private final TaskScheduler taskScheduler;

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
                        System.out.println("Exporting node: " + application.label());
                        eventPublisher.publishEvent(event);
                    };

                    ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_EXPORT_FREQUENCY).toString()));
                }).subscribe();
    }

//    @Scheduled(cron = "* * * * * *")
//    public void scheduled() {
//        applicationsService.listApplications(new AdminToken())
//                .doOnNext(node -> {
//                    JobScheduledEvent event = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), node);
//                    eventPublisher.publishEvent(event);
//                }).subscribe();
//    }

    @Override
    public void onApplicationEvent(ApplicationCreatedEvent event) {
//        applicationsService.getApplication(event.getApplication(), new AdminToken())
//                .subscribe(newApplication -> {
        if (!event.getApplication().configuration().containsKey(CONFIG_KEY_EXPORT_FREQUENCY)) return;
        Runnable task = () -> {
            JobScheduledEvent jobEvent = new ApplicationJobScheduledEvent("exportApplication", new AdminToken(), event.getApplication());
            eventPublisher.publishEvent(jobEvent);
        };
        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(event.getApplication().configuration().get(CONFIG_KEY_EXPORT_FREQUENCY).toString()));
//                });
    }
}
