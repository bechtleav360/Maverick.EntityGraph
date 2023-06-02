package org.av360.maverick.graph.feature.applications.domain.model;
import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import reactor.core.publisher.Mono;

public class QueryVariables {

    public static final Variable varAppIri = SparqlBuilder.var("appNode");
    public static final Variable varAppKey = SparqlBuilder.var("appId");
    public static final Variable varAppLabel = SparqlBuilder.var("appLabel");
    public static final Variable varAppFlagPersistent = SparqlBuilder.var("appPersistent");
    public static final Variable varAppFlagPublic = SparqlBuilder.var("appPublic");
    public static final Variable varAppFlagS3Host = SparqlBuilder.var("appS3Host");
    public static final Variable varAppFlagS3BucketId = SparqlBuilder.var("appS3BucketId");
    public static final Variable varAppFlagExportFrequency = SparqlBuilder.var("appExportFrequency");

    public static final Variable varSubIri = SparqlBuilder.var("subNode");
    public static final Variable varSubKey = SparqlBuilder.var("subId");
    public static final Variable varSubIssued = SparqlBuilder.var("subIssued");
    public static final Variable varSubActive = SparqlBuilder.var("subActive");
    public static final Variable varSubLabel = SparqlBuilder.var("subLabel");


    public static Mono<Subscription> buildSubscriptionFromBindings(BindingsAccessor ba)  {
        return buildApplicationFromBindings(ba)
                .flatMap(application ->
                        buildSubscriptionFromBindings(ba, application)
                );
    }


    public static  Mono<Subscription> buildSubscriptionFromBindings(BindingsAccessor ba, Application app) {
        try {
            Subscription subscription = new Subscription(
                    ba.asIRI(varSubIri),
                    ba.asString(varSubLabel),
                    ba.asString(varSubKey),
                    ba.asBoolean(varSubActive),
                    ba.asString(varSubIssued),
                    app
            );
            return Mono.just(subscription);
        } catch (InconsistentModelException e) {
            return Mono.error(e);
        }



    public static Mono<Application> buildApplicationFromBindings(BindingsAccessor ba) {
        try {
            Application application =  new Application(
                    ba.asIRI(varAppIri),
                    ba.asString(varAppLabel),
                    ba.asString(varAppKey),
                    new ApplicationFlags(
                            ba.asBoolean(varAppFlagPersistent),
                            ba.asBoolean(varAppFlagPublic),
                            ba.asString(varAppFlagS3Host),
                            ba.asString(varAppFlagS3BucketId),
                            ba.asString(varAppFlagExportFrequency)
                    )
            );
            return Mono.just(application);
        } catch (InconsistentModelException e) {
            return Mono.error(e);
        }
    }
}
