package org.av360.maverick.graph.feature.jobs.schedulers;


import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.jobs.services.DetectDuplicatesService;
import org.av360.maverick.graph.model.security.AdminToken;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
@ConditionalOnProperty(name = "application.features.modules.jobs.scheduled.detectDuplicates", havingValue = "true")
public class ScheduledDetectDuplicates extends  ScheduledJob {

    private final DetectDuplicatesService detectDuplicates;

    public ScheduledDetectDuplicates(DetectDuplicatesService detectDuplicates) {
        this.detectDuplicates = detectDuplicates;
    }


    // https://github.com/spring-projects/spring-framework/issues/23533

    @Scheduled(initialDelay = 60000, fixedRate = 120000)
    public void checkForDuplicatesScheduled() {
        AdminToken adminAuthentication = new AdminToken();
        Mono<Void> job = this.detectDuplicates.checkForDuplicates(RDFS.LABEL, adminAuthentication)
                .then(this.detectDuplicates.checkForDuplicates(SDO.IDENTIFIER, adminAuthentication))
                .then(this.detectDuplicates.checkForDuplicates(SKOS.PREF_LABEL, adminAuthentication))
                .then(this.detectDuplicates.checkForDuplicates(DCTERMS.IDENTIFIER, adminAuthentication))
                .then(this.detectDuplicates.checkForDuplicates(DC.IDENTIFIER, adminAuthentication));
        super.schedule(job, "detect duplicates");

    }
}