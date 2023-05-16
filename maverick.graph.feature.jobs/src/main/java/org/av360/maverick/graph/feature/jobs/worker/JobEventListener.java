package org.av360.maverick.graph.feature.jobs.worker;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Slf4j(topic = "graph.jobs")
@Service
public class JobEventListener implements Publisher<JobScheduledEvent>, ApplicationListener<JobScheduledEvent> {

    Subscriber<? super JobScheduledEvent> subscriber;
    private MeterRegistry meterRegistry;

    @Autowired
    private void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {
        meterRegistry.counter("graph.scheduled.jobs.counter", "name", event.getJobIdentifier(), "event", "received").increment();
        // see https://github.com/spring-projects/spring-framework/pull/29924
        if(this.subscriber != null) {
            try {
                this.subscriber.onNext(event);
            } catch (Exception e) {
                log.warn("Exception while broadcasting job scheduled event to worker with message: {}", e.getMessage());
            }

        }

    }

    @Override
    public void subscribe(Subscriber<? super JobScheduledEvent> subscriber) {
        this.subscriber = subscriber;
    }
}
