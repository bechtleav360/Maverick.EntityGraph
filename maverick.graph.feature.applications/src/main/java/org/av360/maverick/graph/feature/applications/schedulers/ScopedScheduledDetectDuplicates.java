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

/**
 * Regular check for duplicates in the entity stores.
 * <p>
 * A typical example for a duplicate are the following two entities uploaded on different times
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * They both share the defined term "Term 1". Since they are uploaded in different requests, we don't check for duplicates. The (embedded) entity
 * <p>
 * x a DefinedTerm
 * label "Term 1"
 * <p>
 * is therefore a duplicate in the repository after the second upload. This scheduler will check for these duplicates by looking at objects which
 * - share the same label
 * - share the same original_identifier
 * <p>
 * <p>
 *  TODO:
 *      For now we keep the duplicate but reroute all links to the original.
 */
@Component
@Slf4j(topic = "graph.jobs.duplicates")
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.detectDuplicates.enabled", havingValue = "true")
public class ScopedScheduledDetectDuplicates  {

    public static final String CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY = "detect_duplicates_frequency";

    private final ApplicationEventPublisher eventPublisher;

    private final ApplicationsService applicationsService;

    private final TaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();


    public ScopedScheduledDetectDuplicates(ApplicationEventPublisher eventPublisher, ApplicationsService applicationsService, TaskScheduler taskScheduler) {
        this.eventPublisher = eventPublisher;
        this.applicationsService = applicationsService;
        this.taskScheduler = taskScheduler;
    }

    private void _scheduleRunnableTask(Application application) {
        if (!application.configuration().containsKey(CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY)) return;

        Runnable task = () -> {
            JobScheduledEvent jobEvent = new ApplicationJobScheduledEvent("detectDuplicates", new AdminToken(), application.label());
            eventPublisher.publishEvent(jobEvent);
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(application.configuration().get(CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY).toString()));
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
//            .subscribe(newApplication -> {
                    _scheduleRunnableTask(event.getApplication());
//            });
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