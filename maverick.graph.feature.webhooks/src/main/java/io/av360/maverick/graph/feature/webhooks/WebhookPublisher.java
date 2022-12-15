package io.av360.maverick.graph.feature.webhooks;

import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Service;

public interface WebhookPublisher {
    public void publish(CloudEvent ce);
}
