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
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * If we have any global identifiers (externally set) in the repo, we have to replace them with our internal identifiers.
 * Otherwise we cannot address the entities through our API.
 * <p>
 * Periodically runs the following sparql queries, grabs the entity definition for it and regenerates the identifiers
 * <p>
 * SELECT ?a WHERE { ?a a ?c . }
 * FILTER NOT EXISTS {
 * FILTER STRSTARTS(str(?a), "http://graphs.azurewebsites.net/api/entities/").
 * }
 * LIMIT 100
 */
@Slf4j(topic = "graph.jobs.identifiers")
@Component
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.replaceIdentifiers.enabled", havingValue = "true")
public class ScopedScheduledReplaceIdentifiers {

    // FIXME: should not directly access the services

    public static final String CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY = "replace_identifiers_frequency";
    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationsService applicationsService;

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    public ScopedScheduledReplaceIdentifiers(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService, TaskScheduler taskScheduler) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
        this.taskScheduler = taskScheduler;
    }

    private void _scheduleRunnableTask(Application application) {
        if (!application.configuration().containsKey(CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY)) return;

        Runnable task = () -> {
            JobScheduledEvent event = new ApplicationJobScheduledEvent("replaceSubjectIdentifiers", new AdminToken(), application);
            eventPublisher.publishEvent(event);
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_REPLACE_IDENTIFIERS_FREQUENCY).toString()));
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