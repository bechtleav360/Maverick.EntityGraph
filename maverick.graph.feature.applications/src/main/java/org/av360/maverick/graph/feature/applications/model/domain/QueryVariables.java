package org.av360.maverick.graph.feature.applications.model.domain;

import org.av360.maverick.graph.model.errors.InconsistentModelException;
import org.av360.maverick.graph.store.rdf.helpers.BindingsAccessor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import reactor.core.publisher.Mono;

import java.util.HashMap;

public class QueryVariables {

    public static final Variable varNodeApplication = SparqlBuilder.var("appNode");
    public static final Variable varAppKey = SparqlBuilder.var("appId");
    public static final Variable varAppLabel = SparqlBuilder.var("appLabel");
    public static final Variable varAppFlagPersistent = SparqlBuilder.var("appPersistent");
    public static final Variable varAppFlagPublic = SparqlBuilder.var("appPublic");
    public static final Variable varAppKeyword = SparqlBuilder.var("appTag");
    public static final Variable varNodeConfigurationItem = SparqlBuilder.var("configItem");
    public static final Variable varAppFlagS3Host = SparqlBuilder.var("appS3Host");
    public static final Variable varAppFlagS3BucketId = SparqlBuilder.var("appS3BucketId");
    public static final Variable varAppFlagExportFrequency = SparqlBuilder.var("appExportFrequency");

    public static final Variable varConfigurationItems = SparqlBuilder.var("appConfigItem");

    public static final Variable varNodeSubscription = SparqlBuilder.var("subNode");
    public static final Variable varSubKey = SparqlBuilder.var("subId");


    public static final Variable varSubIssued = SparqlBuilder.var("subIssued");
    public static final Variable varSubActive = SparqlBuilder.var("subActive");
    public static final Variable varSubLabel = SparqlBuilder.var("subLabel");
    public static final Variable varConfigKey = SparqlBuilder.var("cfgL");

    public static final Variable varConfigValue = SparqlBuilder.var("cfgV");


    public static Mono<Subscription> buildSubscriptionFromBindings(BindingsAccessor ba)  {
        return buildApplicationFromBindings(ba)
                .flatMap(application ->
                        buildSubscriptionFromBindings(ba, application)
                );
    }


    public static  Mono<Subscription> buildSubscriptionFromBindings(BindingsAccessor ba, Application app) {
        try {
            Subscription subscription = new Subscription(
                    ba.asIRI(varNodeSubscription),
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
    }



    public static Mono<Application> buildApplicationFromBindings(BindingsAccessor ba) {
        try {
            Application application =  new Application(
                    ba.asIRI(varNodeApplication),
                    ba.asString(varAppLabel),
                    ba.asString(varAppKey),
                    ba.asSet(varAppKeyword.getVarName()),
                    new ApplicationFlags(
                            ba.asBoolean(varAppFlagPersistent),
                            ba.asBoolean(varAppFlagPublic)
                    ),
                    new HashMap<>()
            );
            return Mono.just(application);
        } catch (InconsistentModelException e) {
            return Mono.error(e);
        }
    }

    public static Mono<ConfigurationItem> buildConfigurationItemFromBindings(BindingsAccessor bindingsAccessor) {

        try {
            ConfigurationItem result = new ConfigurationItem(
                    bindingsAccessor.asIRI(varNodeConfigurationItem),
                    bindingsAccessor.asString(varConfigKey),
                    bindingsAccessor.asString(varConfigValue),
                    bindingsAccessor.findValue(varNodeApplication).map(value -> (IRI) value).orElse(SimpleValueFactory.getInstance().createIRI("http://example.org"))
            );
            return Mono.just(result);
        } catch (InconsistentModelException e) {
            return Mono.error(e);
        }

    }
}
