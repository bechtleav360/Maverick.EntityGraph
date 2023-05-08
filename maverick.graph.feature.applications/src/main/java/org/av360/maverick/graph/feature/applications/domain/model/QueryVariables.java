package org.av360.maverick.graph.feature.applications.domain.model;

import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;

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


    public static Subscription buildSubscriptionFromBindings(BindingsAccessor ba) {

        return new Subscription(
                ba.asIRI(varSubIri),
                ba.asString(varSubLabel),
                ba.asString(varSubKey),
                ba.asBoolean(varSubActive),
                ba.asString(varSubIssued),
                buildApplicationFromBindings(ba)
        );
    }


    public static  Subscription buildSubscriptionFromBindings(BindingsAccessor ba, Application app) {

        return new Subscription(
                ba.asIRI(varSubIri),
                ba.asString(varSubLabel),
                ba.asString(varSubKey),
                ba.asBoolean(varSubActive),
                ba.asString(varSubIssued),
                app
        );
    }

    public static Application buildApplicationFromBindings(BindingsAccessor ba) {
        return new Application(
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
    }
}
