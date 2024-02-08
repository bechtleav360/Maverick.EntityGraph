package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.annotations.Job;
import org.av360.maverick.graph.model.entities.ScheduledJob;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.SessionContextBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j(topic = "graph.jobs")
@Job
public class JobWorker {

    private final Set<SessionContextBuilder> builders;

    private final JobQueue requestedJobs;
    private final List<ScheduledJob> registeredJobs;

    private final Deque<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> submittedJobs;

    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskScheduler taskScheduler;


    public JobWorker(Set<SessionContextBuilder> builders, JobQueue eventListener, List<ScheduledJob> jobs, MeterRegistry meterRegistry) {
        this.builders = builders;
        this.requestedJobs = eventListener;
        this.registeredJobs = jobs;
        this.submittedJobs = new ArrayDeque<>();
        this.meterRegistry = meterRegistry;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setThreadGroupName("jobs");

        this.taskScheduler.initialize();
    }

    @Scheduled(fixedRate = 2, timeUnit = TimeUnit.MINUTES)
    public void runJob() {

        if(requestedJobs.peek().isEmpty()) return;


        // we should always have only job running in one scope, to prevent read/write conflicts
        requestedJobs.peek().ifPresent(jobIdentifier -> {

            JobScheduledEvent event = requestedJobs.accept().orElseThrow();

            // check if job with this identifier is already scheduled or active
            Optional<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> alreadyScheduledJob = this.submittedJobs.stream()
                    .filter(scheduledJob -> ! scheduledJob.isCompleted())
                    .filter(scheduledJob -> ! scheduledJob.isFailed())
                    .filter(scheduledJob -> scheduledJob.getIdentifier().equalsIgnoreCase(event.getJobIdentifier())).findFirst();
            if(alreadyScheduledJob.isPresent()) {
                if(alreadyScheduledJob.get().isSubmitted()) return;
                if(alreadyScheduledJob.get().isActive()) this.requestedJobs.delayFirst();
            }


            ScheduledJob requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst().orElseThrow();


            Flux.fromIterable(this.builders)
                    .reduceWith(() -> Mono.just(event.getSessionContext()), (update, builderService) -> update.flatMap(builderService::build)).flatMap(mono -> mono)
                    // jobs always run with System authentication
                    .doOnNext(ctx -> ctx.withAuthority(Authorities.MAINTAINER))
                    .doOnNext(context -> {


                        // else create a new scheduled Job
                        log.debug("Scheduling job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                        org.av360.maverick.graph.feature.jobs.model.ScheduledJob scheduledJob = new org.av360.maverick.graph.feature.jobs.model.ScheduledJob(requestedJob, context, event.getJobIdentifier());



                        Mono.fromFuture(this.taskScheduler.submitCompletable(scheduledJob))
                                .doOnSubscribe(subscription -> {
                                    scheduledJob.setSubmitted();
                                    this.submittedJobs.addFirst(scheduledJob);

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



    public List<ScheduledJob> getRegisteredJobs() {
        return registeredJobs;
    }

    public List<JobScheduledEvent> getRequestedJobs() {
        return this.requestedJobs.list();
    }


    public List<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> getActiveJobs() {
        return this.submittedJobs.stream().filter(org.av360.maverick.graph.feature.jobs.model.ScheduledJob::isActive).collect(Collectors.toList());
    }

    public List<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> getSubmittedJobs() {
        return this.submittedJobs.stream().filter(org.av360.maverick.graph.feature.jobs.model.ScheduledJob::isSubmitted).collect(Collectors.toList());
    }



    public List<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> getFailedJobs() {
        return this.submittedJobs.stream().filter(org.av360.maverick.graph.feature.jobs.model.ScheduledJob::isFailed).collect(Collectors.toList());
    }

    public List<org.av360.maverick.graph.feature.jobs.model.ScheduledJob> getCompletedJobs() {
        return this.submittedJobs.stream().filter(org.av360.maverick.graph.feature.jobs.model.ScheduledJob::isCompleted).collect(Collectors.toList());
    }

}
