package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
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

    private final List<Job> submittedJobs;

    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskScheduler taskScheduler;


    public JobWorker(Set<SessionContextBuilderService> builders, JobQueue eventListener, List<Job> jobs, MeterRegistry meterRegistry) {
        this.builders = builders;
        this.requestedJobs = eventListener;
        this.registeredJobs = jobs;
        this.submittedJobs = new ArrayList<>();
        this.meterRegistry = meterRegistry;
        this.scheduler = Schedulers.newBoundedElastic(2, 10, "jobs");
        this.taskScheduler = new TaskSchedulerBuilder().poolSize(3).threadNamePrefix("jobs").build();

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
                        log.debug("Scheduling job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                        Job job = requestedJob.get().withContext(context).withIdentifier(event.getJobIdentifier());

                        Mono.fromFuture(this.taskScheduler.submitCompletable(job))
                                .doOnSubscribe(subscription -> {
                                    job.setSubmitted();
                                    this.submittedJobs.add(job);

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


    public List<Job> getActiveJobs() {
        return this.submittedJobs.stream().filter(Job::isActive).collect(Collectors.toList());
    }

    public List<Job> getSubmittedJobs() {
        return this.submittedJobs.stream().filter(Job::isSubmitted).collect(Collectors.toList());
    }

    public List<Job> getFailedJobs() {
        return this.submittedJobs.stream().filter(Job::isFailed).limit(5).collect(Collectors.toList());
    }

    public List<Job> getCompletedJobs() {
        return this.submittedJobs.stream().filter(Job::isCompleted).limit(5).collect(Collectors.toList());
    }

}
