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

/**
 * @see Sch
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.typeCoercion.enabled", havingValue = "true")
public class ScopedScheduledTypeCoercion implements ApplicationListener<ApplicationCreatedEvent> {
    public static final String CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY = "assign_internal_types_frequency";
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationsService applicationsService;
    private final TaskScheduler taskScheduler;

    public ScopedScheduledTypeCoercion(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService, TaskScheduler taskScheduler) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void initializeScheduledJobs() {
        applicationsService.listApplications(new AdminToken())
                .doOnNext(application -> {
                    Runnable task = () -> {
                        JobScheduledEvent event = new ApplicationJobScheduledEvent("typeCoercion", new AdminToken(), application.label());
                        eventPublisher.publishEvent(event);
                    };
                    ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY).toString()));
                }).subscribe();
    }

    @Override
    public void onApplicationEvent(ApplicationCreatedEvent event) {
//        applicationsService.getApplication(event.getApplication(), new AdminToken())
//                .subscribe(newApplication -> {
                    Runnable task = () -> {
                        JobScheduledEvent jobEvent = new ApplicationJobScheduledEvent("typeCoercion", new AdminToken(), event.getApplication().label());
                        eventPublisher.publishEvent(jobEvent);
                    };
                    ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(event.getApplication().configuration().get(CONFIG_KEY_ASSIGN_INTERNAL_TYPES_FREQUENCY).toString()));
//                });
    }

}