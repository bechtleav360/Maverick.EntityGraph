package org.av360.maverick.graph.model.enums;

import org.av360.maverick.graph.model.vocabulary.meg.Local;

public enum RepositoryType {
    ENTITIES,
    SCHEMA,
    TRANSACTIONS,
    APPLICATION,
    CLASSIFIER, UNSET;


    @Override
    public String toString() {
        return name().toLowerCase();
    }


    public String getIdentifierNamespace() {
        switch (this) {
            case ENTITIES -> {
                return Local.Entities.NAME;
            }
            case APPLICATION -> {
                return Local.Applications.NAME;
            }
            case TRANSACTIONS -> {
                return Local.Transactions.NAME;
            }
            case SCHEMA -> {
                return Local.Schema.NAME;
            }
            case CLASSIFIER -> {
                return Local.Classifier.NAME;
            }
            default -> {
                return Local.NAMESPACE;
            }
        }
    }
}