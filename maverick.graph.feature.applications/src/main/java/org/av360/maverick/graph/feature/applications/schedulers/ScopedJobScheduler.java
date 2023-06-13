package org.av360.maverick.graph.feature.applications.schedulers;

import jakarta.annotation.PostConstruct;
import org.av360.maverick.graph.feature.applications.domain.ApplicationsService;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationCreatedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationDeletedEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationJobScheduledEvent;
import org.av360.maverick.graph.feature.applications.domain.events.ApplicationUpdatedEvent;
import org.av360.maverick.graph.feature.applications.domain.model.Application;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.AdminToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

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
        applicationsService.listApplications(new AdminToken()).doOnNext(this::scheduleRunnableTask).subscribe();
    }

    protected void scheduleRunnableTask(Application application) {
        String cronTrigger = application.configuration().containsKey(getFrequencyConfigurationKey()) ?
                application.configuration().get(getFrequencyConfigurationKey()).toString() : getDefaultFrequency();

        Runnable task = () -> {
            JobScheduledEvent event = new ApplicationJobScheduledEvent(getJobLabel(), new AdminToken(), application.label());
            eventPublisher.publishEvent(event);
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(task, new CronTrigger(cronTrigger));
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
