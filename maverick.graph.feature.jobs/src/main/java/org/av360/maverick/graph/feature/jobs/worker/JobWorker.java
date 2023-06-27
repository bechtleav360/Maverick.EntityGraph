package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.av360.maverick.graph.model.security.Authorities;
import org.av360.maverick.graph.services.SessionContextBuilderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private final Set<SessionContextBuilderService> builders;

    private final Scheduler scheduler;
    private final Map<String, String> activeJobs;
    private final JobQueue requestedJobs;
    private final List<Job> registeredJobs;

    private final MeterRegistry meterRegistry;


    public JobWorker(Set<SessionContextBuilderService> builders, JobQueue eventListener, List<Job> jobs, MeterRegistry meterRegistry) {
        this.builders = builders;
        this.requestedJobs = eventListener;
        this.registeredJobs = jobs;
        this.meterRegistry = meterRegistry;
        this.scheduler = Schedulers.newBoundedElastic(5, 10, "jobs");
        this.activeJobs = new HashMap<>();
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.SECONDS)
    public void runJob() {
        if(requestedJobs.peek().isEmpty()) return;


        // we should always have only job running in one scope, to prevent read/write conflicts
        requestedJobs.peek().ifPresent(jobIdentifier -> {
            if(StringUtils.hasLength(this.activeJobs.get(jobIdentifier.scope()))) {
                log.debug("Job '{}' in scope '{}' still running, skipping scheduled run of job '{}' in same scope.", this.activeJobs.get(jobIdentifier.scope()), jobIdentifier.scope(), jobIdentifier.name());
                requestedJobs.delayFirst();
                meterRegistry.counter("graph.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "status", "hold").increment();
                return;
             }

            JobScheduledEvent event = requestedJobs.accept().orElseThrow();
            Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
            if (requestedJob.isEmpty()) {
                log.warn("Job with label '{}' requested, but not active.", event.getJobName());
                return;
            }


            Flux.fromIterable(this.builders)
                    .reduceWith(() -> Mono.just(event.getSessionContext()), (update, builderService) -> update.flatMap(builderService::build)).flatMap(mono -> mono)
                    // jobs always run with System authentication
                    .doOnNext(ctx -> ctx.withAuthority(Authorities.MAINTAINER))
                    .flatMap(ctx -> requestedJob.get().run(ctx))
                    .subscribeOn(scheduler)
                    .doOnSubscribe(subscription -> {
                        log.debug("Starting job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                        this.activeJobs.put(jobIdentifier.scope(), jobIdentifier.name());
                    })
                    .doOnSuccess(success -> {
                        log.trace("Completed job '{}' in {}.", event.getJobIdentifier(), event.getSessionContext().getEnvironment());
                        this.activeJobs.put(jobIdentifier.scope(), null);
                        meterRegistry.counter("graph.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "status", "completed").increment();
                    })
                    .doOnError(error -> {
                        log.warn("Failed job '{}' in {} due to reason: {}", event.getJobIdentifier(), event.getSessionContext().getEnvironment(), error.getMessage());
                        meterRegistry.counter("graph.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "status ", "failed").increment();
                        this.activeJobs.put(jobIdentifier.scope(), null);
                    }).subscribe();


        });






    }



    public List<Job> getRegisteredJobs() {
        return registeredJobs;
    }


}
