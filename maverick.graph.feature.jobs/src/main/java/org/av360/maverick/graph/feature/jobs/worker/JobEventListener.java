package org.av360.maverick.graph.feature.jobs.worker;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.events.JobScheduledEvent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Slf4j(topic = "graph.jobs")
@Service
public class JobEventListener implements Publisher<JobScheduledEvent>, ApplicationListener<JobScheduledEvent> {

    Subscriber<? super JobScheduledEvent> subscriber;

    @Override
    public void onApplicationEvent(JobScheduledEvent event) {
        if(this.subscriber != null) {
            this.subscriber.onNext(event);
        }

    }

    @Override
    public void subscribe(Subscriber<? super JobScheduledEvent> subscriber) {
        this.subscriber = subscriber;
    }
}
