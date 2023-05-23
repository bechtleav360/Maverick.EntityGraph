package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private final Scheduler scheduler;
    private String active;
    private final JobQueue queue;
    private final List<Job> jobs;

    private final MeterRegistry meterRegistry;


    public JobWorker(JobQueue eventListener, List<Job> jobs, MeterRegistry meterRegistry) {
        this.queue = eventListener;
        this.jobs = jobs;
        this.meterRegistry = meterRegistry;
        this.scheduler = Schedulers.newSingle("jobs");
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void runJob() {
        if (StringUtils.hasLength(this.active)) {
            log.debug("Job '{}' still running, skipping scheduled run of job '{}'.", this.active, queue.peek());
            meterRegistry.counter("graph.scheduled.jobs.counter", "name", queue.peek(), "result", "hold").increment();
            return;
        }

        JobScheduledEvent event = this.queue.accept();
        Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
        if (requestedJob.isEmpty()) {
            log.warn("Job with label '{}' requested, but not active.", event.getJobName());
            return;
        }

        requestedJob.get().run(event.getToken())
                .subscribeOn(scheduler)
                .contextWrite(event::buildContext)
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
                }).subscribe();
    }



    public List<Job> getRegisteredJobs() {
        return jobs;
    }


}
