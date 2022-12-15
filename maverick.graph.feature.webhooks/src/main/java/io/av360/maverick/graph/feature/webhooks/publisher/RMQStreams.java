package io.av360.maverick.graph.feature.webhooks.publisher;


import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.MessageBuilder;
import com.rabbitmq.stream.Producer;
import io.av360.maverick.graph.feature.webhooks.WebhookPublisher;
import io.cloudevents.CloudEvent;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RMQStreams implements WebhookPublisher {
    final Producer producer;

    public RMQStreams() {
        Environment environment = this.initEnvironment();
         this.producer = environment.producerBuilder()
                .stream("events")
                .build();

    }

    private Environment initEnvironment() {
        return Environment.builder()
                .host("rabbitmq")
                .port(5672)
                .virtualHost("/")
                .username("user")
                .password("AUzpCUjq4U")
                .build();
    }


    @Override
    public void publish(CloudEvent ce) {
        MessageBuilder messageBuilder = this.producer.messageBuilder();
        messageBuilder.publishingId(Long.parseLong(ce.getId()));
        messageBuilder.addData(Objects.requireNonNull(ce.getData()).toBytes());

        this.producer.send(messageBuilder.build(), confirmationStatus  -> {
            if (confirmationStatus.isConfirmed()) {
                // the message made it to the broker
            } else {
                // the message did not make it to the broker
                // TODO: add it to local dead letter queue
            }
        });

    }
}
