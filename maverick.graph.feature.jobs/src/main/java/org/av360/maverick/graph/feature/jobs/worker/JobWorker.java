package org.av360.maverick.graph.feature.jobs.worker;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker implements ApplicationListener<JobScheduledEvent> {

    private final Set<String> active;
    private List<Job> jobs;

    @Autowired(required = false)
    public void setRegisteredJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    public JobWorker() {
        active = new HashSet<>();
    }

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {
        Optional<Job> requestedJob = this.getRegisteredJobs().stream().filter(job -> job.getName().equalsIgnoreCase(event.getJobName())).findFirst();
        if(requestedJob.isEmpty()) log.warn("Job with label '{}' requested, but not active.", event.getJobName());
        else {
            Mono<Void> mono = requestedJob.get().run(event.getToken()).contextWrite(event::buildContext);
            this.schedule(mono, event.getJobIdentifier());
        }


    }

    // see https://github.com/spring-projects/spring-framework/pull/29924
    private void schedule(Mono<Void> job, String label) {
        if(this.active.contains(label)) {
            log.debug("Job '{}' still running, skipping this scheduled run.", label);
            return;
        }

        job.doOnSubscribe(subscription -> {
                    log.debug("Starting job '{}'", label);
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
                .publishOn(Schedulers.single())
                .subscribe();



    }


    public List<Job> getRegisteredJobs() {
        return jobs;
    }
}
