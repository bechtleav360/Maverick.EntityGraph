package org.av360.maverick.graph.feature.applications.schedulers;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.model.enums.ConfigurationKeysRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Regular check for duplicates in the entity stores.
 * <p>
 * A typical example for a duplicate are the following two entities uploaded on different times
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * <p>
 * [] a ns1:VideoObject ;
 * ns1:hasDefinedTerm [
 * a ns1:DefinedTerm ;
 * rdfs:label "Term 1"
 * ] .
 * <p>
 * They both share the defined term "Term 1". Since they are uploaded in different requests, we don't check for duplicates. The (embedded) entity
 * <p>
 * x a DefinedTerm
 * label "Term 1"
 * <p>
 * is therefore a duplicate in the repository after the second upload. This scheduler will check for these duplicates by looking at objects which
 * - share the same label
 * - share the same original_identifier
 * <p>
 * <p>
 *  TODO:
 *      For now we keep the duplicate but reroute all links to the original.
 */
@Component
@Slf4j(topic = "graph.jobs.duplicates")
@ConditionalOnProperty({"application.features.modules.jobs.scheduled.detectDuplicates.enabled","application.features.modules.applications.enabled"})

public class ScopedScheduledDetectDuplicates extends ScopedJobScheduler {

    @Value("${application.features.modules.jobs.scheduled.detectDuplicates.defaultFrequency:@midnight}")
    String defaultFrequency;

    public static final String CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY = "detect_duplicates_frequency";

    public ScopedScheduledDetectDuplicates() {
        ConfigurationKeysRegistry.add(CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY, "Frequency as Cron Job Pattern for detecting duplicates within an application");
    }

    @Override
    String getFrequencyConfigurationKey() {
        return CONFIG_KEY_DETECT_DUPLICATES_FREQUENCY;
    }

    @Override
    String getJobLabel() {
        return "detectDuplicates";
    }

    @Override
    String getDefaultFrequency() {
        return this.defaultFrequency;
    }


}