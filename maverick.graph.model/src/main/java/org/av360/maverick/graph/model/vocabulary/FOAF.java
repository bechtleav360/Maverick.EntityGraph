package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;

import java.util.Set;

public class FOAF extends org.eclipse.rdf4j.model.vocabulary.FOAF {

    public static Set<IRI> getIndividualTypes() {
        return Set.of(
                FOAF.AGENT,
                FOAF.GROUP,
                FOAF.DOCUMENT,
                FOAF.PERSON,
                FOAF.PROJECT,
                FOAF.ORGANIZATION
        );
    }


    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                FOAF.ACCOUNT_NAME,
                FOAF.AIM_CHAT_ID,
                FOAF.FIRST_NAME,
                FOAF.FAMILY_NAME,
                FOAF.GIVEN_NAME,
                FOAF.JABBER_ID,
                FOAF.ICQ_CHAT_ID,
                FOAF.MSN_CHAT_ID,
                FOAF.NAME,
                FOAF.NICK,
                FOAF.SURNAME,
                FOAF.TITLE,
                FOAF.YAHOO_CHAT_ID
        );
    }

    public static Set<IRI>  getClassifierTypes() {
        return Set.of(

        );
    }
}
