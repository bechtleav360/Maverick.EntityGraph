package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private String active;
    private final JobEventListener eventListener;
    private List<Job> jobs;

    private final Scheduler scheduler;
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    public void setRegisteredJobs(List<Job> jobs) {
        this.jobs = jobs;
    }


    public JobWorker(JobEventListener eventListener) {
        this.eventListener = eventListener;
        this.scheduler = Schedulers.newSingle("jobs");
    }

    @PostConstruct
    public void listen() {
        Flux<Void> voidFlux = Flux.from(eventListener)
                .filter(event -> {
                    meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "accepted").increment();

                    if (StringUtils.hasLength(this.active)) {
                        log.debug("Job '{}' still running, skipping scheduled run of job '{}'.", this.active, event.getJobIdentifier());
                        meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "skipped").increment();
                        return false;
                    } else {
                        return true;
                    }
                })
                .flatMap(event -> {
                    Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
                    if (requestedJob.isEmpty()) {
                        log.warn("Job with label '{}' requested, but not active.", event.getJobName());
                        return Mono.empty();
                    } else {

                        return requestedJob.get().run(event.getToken()).contextWrite(event::buildContext)
                                .doOnSubscribe(subscription -> {
                                    log.debug("Starting job '{}'.", event.getJobIdentifier());
                                    this.active = event.getJobIdentifier();
                                })
                                .doOnSuccess(success -> {
                                    log.trace("Completed job '{}'", event.getJobIdentifier());
                                    this.active = "";
                                    meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "completed").increment();
                                })
                                .doOnError(error -> {
                                    log.warn("Failed job '{}' due to reason: {}", event.getJobIdentifier(), error.getMessage());
                                    meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "failed").increment();
                                    this.active = "";
                                });

                    }
                })
                .doOnError(error -> log.error("Failed to consume event.", error));

        voidFlux.subscribeOn(Schedulers.single())
                .onErrorResume((error) -> Mono.empty())
                .subscribe();
    }



    public List<Job> getRegisteredJobs() {
        return jobs;
    }

    @Autowired
    private void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

    }
}
