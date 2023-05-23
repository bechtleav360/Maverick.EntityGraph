package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j(topic = "graph.jobs")
@Service
public class JobQueue implements ApplicationListener<JobScheduledEvent> {

    private final MeterRegistry meterRegistry;

    private final Deque<JobScheduledEvent> publishedJobs;

    public JobQueue(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.publishedJobs = new ArrayDeque<>();
    }

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {
        meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "event", "received").increment();

        if(this.publishedJobs.contains(event)) {
            this.publishedJobs.add(event);
        }
    }

    public JobScheduledEvent accept() {
        JobScheduledEvent next = publishedJobs.pop();
        meterRegistry.counter("graph.scheduled.jobs.counter", "name", next.getJobIdentifier(), "event", "accepted").increment();
        return next;
    }

    public String peek() {
        return publishedJobs.peek() != null ? publishedJobs.peek().getJobIdentifier() : "";
    }
}
