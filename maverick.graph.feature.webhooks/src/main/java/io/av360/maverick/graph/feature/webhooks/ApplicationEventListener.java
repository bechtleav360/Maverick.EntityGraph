package io.av360.maverick.graph.feature.webhooks;

import io.av360.maverick.graph.services.events.EntityEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ApplicationEventListener implements ApplicationListener<EntityEvent> {

    final List<WebhookPublisher> webhookPublisher;
    final RandomStringGenerator generator;
    private final Random random;

    public ApplicationEventListener(@Autowired  List<WebhookPublisher> webhookPublisher) {
        this.webhookPublisher = webhookPublisher;

        this.random = new Random();


        this.generator = new RandomStringGenerator.Builder()
                .withinRange('a', 'z')
                .build();

    }

    @Override
    public void onApplicationEvent(EntityEvent event) {

        Long id = this.generateIdentifier(event.getTimestamp());
        OffsetDateTime time = this.generateTime(event.getTimestamp());
        String type = event.getType();
        URI source = this.generateSource(event.getPath());

        CloudEvent ce = CloudEventBuilder.v1()
                .withId(id.toString())
                .withTime(time)
                .withType(type)
                .withSource(source)
                .build();

        webhookPublisher.forEach(webhookPublisher -> webhookPublisher.publish(ce));

    }

    private URI generateSource(String path) {
        // TODO: find a way to retrieve current host
        return URI.create("http://graph.example.com/"+path);
    }

    private Long generateIdentifier(long timestamp) {
        return Long.valueOf(timestamp + "_" + random.nextInt(1000, 9999));
    }

    private OffsetDateTime generateTime(long timestamp) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
}
