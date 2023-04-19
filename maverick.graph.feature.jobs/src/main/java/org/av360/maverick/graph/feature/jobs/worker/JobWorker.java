package org.av360.maverick.graph.feature.jobs.worker;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.entities.Job;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

@Slf4j(topic = "graph.jobs")
@Service
public class JobWorker implements ApplicationListener<JobScheduledEvent> {

    private final Set<String> active;

    public JobWorker() {
        active = new HashSet<>();
    }

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {

        Job scheduledJob = event.getScheduledJob();
        Authentication token = event.getToken();
        this.schedule(scheduledJob.run(token), scheduledJob.getName());

    }

    // see https://github.com/spring-projects/spring-framework/pull/29924
    private void schedule(Mono<Void> job, String label) {
        if(this.active.contains(label)) {
            log.debug("Job '{}' still running, skipping this scheduled run.", label);
            return;
        }

        job.doOnSubscribe(subscription -> {
                    log.info("Starting job '{}'", label);
                    this.active.add(label);
                })
                .doOnError(error -> {
                    log.warn("Failed job '{}'", label);
                    this.active.remove(label);
                })
                .doOnSuccess(success -> {
                    log.debug("Completed job '{}'", label);
                    this.active.remove(label);
                })
                .publishOn(Schedulers.single())
                .subscribe();



    }


}
