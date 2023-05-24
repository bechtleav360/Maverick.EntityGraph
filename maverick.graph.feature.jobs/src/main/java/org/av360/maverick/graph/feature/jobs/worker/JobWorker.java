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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker {

    private final Scheduler scheduler;
    private final Map<String, String> activeJobs;
    private final JobQueue requestedJobs;
    private final List<Job> registeredJobs;

    private final MeterRegistry meterRegistry;


    public JobWorker(JobQueue eventListener, List<Job> jobs, MeterRegistry meterRegistry) {
        this.requestedJobs = eventListener;
        this.registeredJobs = jobs;
        this.meterRegistry = meterRegistry;
        this.scheduler = Schedulers.newBoundedElastic(5, 10, "jobs");
        this.activeJobs = new HashMap<>();
    }

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)
    public void runJob() {
        if(requestedJobs.peek().isEmpty()) return;


        // we should always have only job running in one scope, to prevent read/write conflicts
        requestedJobs.peek().ifPresent(jobIdentifier -> {
            if(StringUtils.hasLength(this.activeJobs.get(jobIdentifier.scope()))) {
                log.debug("Job '{}' in scope '{}' still running, skipping scheduled run of job '{}'.", this.activeJobs.get(jobIdentifier.scope()), jobIdentifier.scope(), jobIdentifier.name());
                meterRegistry.counter("graph.scheduled.jobs.counter", "name", jobIdentifier.name(), "scope", jobIdentifier.scope(), "result", "hold").increment();
                return;
             }

            JobScheduledEvent event = requestedJobs.accept().orElseThrow();
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
                        this.activeJobs.put(jobIdentifier.scope(), jobIdentifier.name());
                    })
                    .doOnSuccess(success -> {
                        log.trace("Completed job '{}'", event.getJobIdentifier());
                        this.activeJobs.put(jobIdentifier.scope(), null);
                        meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "completed").increment();
                    })
                    .doOnError(error -> {
                        log.warn("Failed job '{}' due to reason: {}", event.getJobIdentifier(), error.getMessage());
                        meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "result", "failed").increment();
                        this.activeJobs.put(jobIdentifier.scope(), null);
                    }).subscribe();


        });






    }



    public List<Job> getRegisteredJobs() {
        return registeredJobs;
    }


}
