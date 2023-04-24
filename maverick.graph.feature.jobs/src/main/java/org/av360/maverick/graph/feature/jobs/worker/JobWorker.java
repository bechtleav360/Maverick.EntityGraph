package org.av360.maverick.graph.feature.jobs.worker;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private final Set<String> active;
    private final JobEventListener eventListener;
    private List<Job> jobs;

    private final Scheduler scheduler;

    @Autowired(required = false)
    public void setRegisteredJobs(List<Job> jobs) {
        this.jobs = jobs;
    }



    public JobWorker(JobEventListener eventListener) {
        this.eventListener = eventListener;
        this.active = new HashSet<>();
        this.scheduler = Schedulers.newSingle("jobs");
    }

    @PostConstruct
    public void listen() {
        Flux<Void> voidFlux = Flux.from(eventListener)
                .flatMap(event -> {
                    Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
                    if (requestedJob.isEmpty()) {
                        log.warn("Job with label '{}' requested, but not active.", event.getJobName());
                        return Mono.empty();
                    } else {
                        if(this.active.contains(event.getJobIdentifier())) {
                            log.debug("Job '{}' still running, skipping this scheduled run.", event.getJobIdentifier());
                            return Mono.empty();
                        }
                        return requestedJob.get().run(event.getToken()).contextWrite(event::buildContext)
                                .doOnSubscribe(subscription -> {
                                    log.debug("Starting job '{}'. Currently active: {}", event.getJobIdentifier(), this.active);
                                    this.active.add(event.getJobIdentifier());
                                })
                                .doOnSuccess(success -> {
                                    log.trace("Completed job '{}'", event.getJobIdentifier());
                                    this.active.remove(event.getJobIdentifier());
                                })
                                .doOnError(error -> {
                                    log.warn("Failed job '{}'", event.getJobIdentifier());
                                    this.active.remove(event.getJobIdentifier());
                                });

                    }
                })
                .doOnError(error -> log.error("Failed to consume event.", error))
                .onBackpressureBuffer(10)
                .delayElements(Duration.of(10, ChronoUnit.SECONDS));

        voidFlux.subscribe();
    }

    // see https://github.com/spring-projects/spring-framework/pull/29924
    private void schedule(Mono<Void> job, String label) {
        if(this.active.contains(label)) {
            log.debug("Job '{}' still running, skipping this scheduled run.", label);
            return;
        }


        job.publishOn(scheduler)
                .doOnSubscribe(subscription -> {
                    log.debug("Starting job '{}'. Currently active: {}", label, this.active);
                    this.active.add(label);
                })
                .doOnError(error -> {
                    log.warn("Failed job '{}'", label);
                    this.active.remove(label);
                })
                .doOnSuccess(success -> {
                    log.trace("Completed job '{}'", label);
                    this.active.remove(label);
                })
                .subscribeOn(scheduler)
                .subscribe();




    }


    public List<Job> getRegisteredJobs() {
        return jobs;
    }
}
