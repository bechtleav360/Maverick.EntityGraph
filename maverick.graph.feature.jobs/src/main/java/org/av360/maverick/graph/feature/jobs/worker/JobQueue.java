package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

@Slf4j(topic = "graph.jobs")
@Service
public class JobQueue implements ApplicationListener<JobScheduledEvent> {

    private final MeterRegistry meterRegistry;

    private final Deque<JobScheduledEvent> publishedJobs;

    public JobQueue(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.publishedJobs = new ArrayDeque<>();
    }

    /**
     * Moves the first to the end of the queue
     */
    public void delayFirst() {
        if(this.publishedJobs.size() > 1) {
            JobScheduledEvent first = this.publishedJobs.pop();
            this.publishedJobs.addLast(first);

            log.trace("Current job queue: "+this.publishedJobs);
        }
    }

    public List<JobScheduledEvent> list() {
        return publishedJobs.stream().toList();
    }

    public record JobIdentifier(String name, String scope) {}

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {
        meterRegistry.counter("graph.jobs.counter", "name", event.getJobName(), "scope", event.getScope(), "status", "received").increment();

        if(! this.publishedJobs.contains(event)) {
            this.publishedJobs.add(event);
        }
    }

    public Optional<JobScheduledEvent> accept() {
        if(this.publishedJobs.isEmpty()) return Optional.empty();

        JobScheduledEvent event = publishedJobs.pop();
        meterRegistry.counter("graph.jobs.counter", "name", event.getJobName(), "scope", event.getScope(), "status", "accepted").increment();
        return Optional.of(event);
    }

    public Optional<JobIdentifier> peek() {
        return Optional.ofNullable(publishedJobs.peek()).map(event -> new JobIdentifier(event.getJobName(), event.getScope()));
    }


}
