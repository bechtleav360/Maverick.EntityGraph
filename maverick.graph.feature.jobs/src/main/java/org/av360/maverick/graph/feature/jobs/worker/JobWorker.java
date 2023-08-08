package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.model.ScheduledJob;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.SessionContextBuilderService;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private final Set<SessionContextBuilderService> builders;


    private final Scheduler scheduler;
    private final Map<String, String> activeJobs;
    private final JobQueue requestedJobs;
    private final List<Job> registeredJobs;

    private final List<ScheduledJob> submittedJobs;

    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskScheduler taskScheduler;


    public JobWorker(Set<SessionContextBuilderService> builders, JobQueue eventListener, List<Job> jobs, MeterRegistry meterRegistry) {
        this.builders = builders;
        this.requestedJobs = eventListener;
        this.registeredJobs = jobs;
        this.submittedJobs = new ArrayList<>();
        this.meterRegistry = meterRegistry;
        this.scheduler = Schedulers.newBoundedElastic(2, 10, "jobs");
        this.taskScheduler = new TaskSchedulerBuilder().poolSize(1).threadNamePrefix("jobs").build();
        this.taskScheduler.initialize();
        this.activeJobs = new HashMap<>();
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.SECONDS)
    public void runJob() {

        if(requestedJobs.peek().isEmpty()) return;


        // we should always have only job running in one scope, to prevent read/write conflicts
        requestedJobs.peek().ifPresent(jobIdentifier -> {

            JobScheduledEvent event = requestedJobs.accept().orElseThrow();
            Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
            Flux.fromIterable(this.builders)
                    .reduceWith(() -> Mono.just(event.getSessionContext()), (update, builderService) -> update.flatMap(builderService::build)).flatMap(mono -> mono)
                    // jobs always run with System authentication
                    .doOnNext(ctx -> ctx.withAuthority(Authorities.MAINTAINER))
                    .doOnNext(context -> {
                        // check if job with this identifier is already scheduled

                        // else create a new scheduled Job
                        log.debug("Scheduling job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                        ScheduledJob scheduledJob = new ScheduledJob(requestedJob.get(), context, event.getJobIdentifier());



                        Mono.fromFuture(this.taskScheduler.submitCompletable(scheduledJob))
                                .doOnSubscribe(subscription -> {
                                    scheduledJob.setSubmitted();
                                    this.submittedJobs.add(scheduledJob);

                                })
                                .doOnSuccess(success -> {
                                    log.trace("Completed job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                                    meterRegistry.counter("graph.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "status", "completed").increment();
                                })
                                .doOnError(error -> {
                                    log.warn("Failed job '{}' in {} due to reason: {}", event.getJobIdentifier(), event.getSessionContext().getEnvironment(), error.getMessage());
                                    meterRegistry.counter("graph.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "status ", "failed").increment();
                                    //this.activeJobs.put(jobIdentifier.scope(), null);
                                }).subscribe();

                    })
                    .subscribe();

        });

    }



    public List<Job> getRegisteredJobs() {
        return registeredJobs;
    }


    public List<ScheduledJob> getActiveJobs() {
        return this.submittedJobs.stream().filter(ScheduledJob::isActive).collect(Collectors.toList());
    }

    public List<ScheduledJob> getSubmittedJobs() {
        return this.submittedJobs.stream().filter(ScheduledJob::isSubmitted).collect(Collectors.toList());
    }

    public List<ScheduledJob> getFailedJobs() {
        return this.submittedJobs.stream().filter(ScheduledJob::isFailed).limit(5).collect(Collectors.toList());
    }

    public List<ScheduledJob> getCompletedJobs() {
        List<ScheduledJob> collect = this.submittedJobs.stream().filter(ScheduledJob::isCompleted).limit(5).collect(Collectors.toList());
        return collect;
    }

}
