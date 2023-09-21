package org.av360.maverick.graph.store.rdf4j.repository.builder.base;

import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class MonitoredStoreBuilder extends AbstractStoreBuilder {


    protected MeterRegistry meterRegistry;

    @Autowired
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {

        if(Objects.nonNull(this.meterRegistry)) {
            Gauge.builder("graph.store.repository.cache", super.getCache(), Cache::estimatedSize)
                    .tag("metric", "estimatedSize")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", super.getCache(), cache -> cache.stats().evictionCount())
                    .tag("metric", "evictionCount")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", super.getCache(), cache -> cache.stats().loadCount())
                    .tag("metric", "loadCount")
                    .register(this.meterRegistry);

            Gauge.builder("graph.store.repository.cache", super.getCache(), cache -> cache.stats().hitCount())
                    .tag("metric", "hitCount")
                    .register(this.meterRegistry);
        }
    }
}
