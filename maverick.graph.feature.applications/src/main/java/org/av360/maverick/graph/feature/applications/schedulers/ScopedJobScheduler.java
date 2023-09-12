package org.av360.maverick.graph.feature.applications.schedulers;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.services.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.services.model.Application;
import org.av360.maverick.graph.model.context.SessionContext;
import org.av360.maverick.graph.model.enums.RepositoryType;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j(topic = "graph.feat.apps.schedulers")
public abstract class ScopedJobScheduler {

    private ApplicationEventPublisher eventPublisher;

    private TaskScheduler taskScheduler;

    private ApplicationsService applicationsService;

    private final Map<String, ScheduledFuture<?>> scheduledTasks;

    protected ScopedJobScheduler() {
        this.scheduledTasks = new ConcurrentHashMap<>();
    }

    abstract String getFrequencyConfigurationKey();

    abstract String getJobLabel();

    abstract String getDefaultFrequency();


    @PostConstruct
    public void initializeScheduledJobs() {
        applicationsService.listApplications(new SessionContext().setSystemAuthentication()).doOnNext(this::scheduleRunnableTask).subscribe();
    }

    protected void scheduleRunnableTask(Application application) {
        String cronTrigger = application.configuration().containsKey(getFrequencyConfigurationKey()) ?
                application.configuration().get(getFrequencyConfigurationKey()).toString() : getDefaultFrequency();

        if(! StringUtils.hasLength(cronTrigger)) {
            return;
        }

        Runnable task = () -> {
            JobScheduledEvent event = new JobScheduledEvent(getJobLabel(), new SessionContext().setSystemAuthentication().updateEnvironment(env -> env.withScope(application.label()).withRepositoryType(RepositoryType.ENTITIES)));
            eventPublisher.publishEvent(event);
        };

        ScheduledFuture<?> scheduledFuture;
        try {
            scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronTrigger));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse expression {}, falling back to default. ", cronTrigger);
            scheduledFuture = taskScheduler.schedule(task, new CronTrigger(getDefaultFrequency()));
        }

        scheduledTasks.put("%s.%s".formatted(application.label(), getJobLabel()), scheduledFuture);
    }


    protected void deleteScheduledTask(String applicationLabel, boolean mayInterruptIfRunning) {
        ScheduledFuture<?> scheduledFuture = scheduledTasks.get("%s.%s".formatted(applicationLabel, getJobLabel()));
        if (scheduledFuture != null) {
            scheduledFuture.cancel(mayInterruptIfRunning);
            scheduledTasks.remove(applicationLabel);
        }
    }

    @EventListener
    public void handleApplicationCreated(ApplicationCreatedEvent event) {
        scheduleRunnableTask(event.getApplication());
    }

    @EventListener
    public void handleApplicationUpdated(ApplicationUpdatedEvent event) {
        deleteScheduledTask(event.getApplication().label(), false);
        scheduleRunnableTask(event.getApplication());
    }

    @EventListener
    public void handleApplicationDeleted(ApplicationDeletedEvent event) {
        deleteScheduledTask(event.getApplicationLabel(), true);
    }

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Autowired
    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Autowired
    public void setApplicationsService(ApplicationsService applicationsService) {
        this.applicationsService = applicationsService;
    }
}
