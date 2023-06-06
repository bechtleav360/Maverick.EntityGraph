package org.av360.maverick.graph.model.vocabulary;

import org.av360.maverick.graph.model.rdf.EntityNamespace;
import org.av360.maverick.graph.model.rdf.LocalIRI;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;

import java.util.Set;

public class ESCO {


    public static final String NAMESPACE = "http://data.europa.eu/esco/model#";
    public static final String PREFIX = "esco";
    public static final Namespace NS = EntityNamespace.of(PREFIX, NAMESPACE);

    public final static IRI  ACCREDITATION = LocalIRI.from(NAMESPACE, "Accreditation");
    public final static IRI  AWARDING_ACTIVITY = LocalIRI.from(NAMESPACE, "AwardingActivity");

    public final static IRI  AWARDING_BODY = LocalIRI.from(NAMESPACE, "AwardingBody");

    public final static IRI  ENTRY_REQUIREMENT = LocalIRI.from(NAMESPACE, "EntryRequirement");
    /*
     * The class of ESCO Skill concepts.
     */
    public final static IRI  SKILL = LocalIRI.from(NAMESPACE, "Skill");
    public final static IRI  OCCUPATION = LocalIRI.from(NAMESPACE, "Occupation");

    /*
     * A qualification is a formal outcome of an assessment and validation process, which is obtained when a competent
     * body determines that an individual has achieved learning outcomes to given standards.
     */
    public final static IRI  QUALIFICATION = LocalIRI.from(NAMESPACE, "Qualification");
    public final static IRI  WORK_CONTEXT = LocalIRI.from(NAMESPACE, "Accreditation");
    /*
     *  The recognition class is used to specify information related to the formal recognition of a qualification
     *  and/or awarding body. It is used to      *  model a national or regional authority that formally recognises
     *  a qualification and/or a certain awarding body
     *  a qualification and/or a certain awarding body
     */
    public final static IRI  RECOGNITION = LocalIRI.from(NAMESPACE, "Recognition");


    public ESCO() {
    }

    public static Set<IRI> getIndividualTypes() {
        return Set.of(
                ESCO.ACCREDITATION,
                ESCO.SKILL,
                ESCO.QUALIFICATION,
                ESCO.OCCUPATION,
                ESCO.RECOGNITION
        );
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(

        );
    }

    public static Set<IRI> getClassifierTypes() {
        return Set.of(

        );
    }
}

