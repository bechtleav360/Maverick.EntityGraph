package org.av360.maverick.graph.model.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.Set;

public class SDO {



    private static final ValueFactory vf = SimpleValueFactory.getInstance();


    public static final String NAMESPACE = "https://schema.org/";
    public static final String PREFIX = "sdo";
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public static final IRI IN_LANGUAGE = vf.createIRI(NAMESPACE, "inLanguage");
    public static final IRI CONTENT_LOCATION = vf.createIRI(NAMESPACE, "contentLocation");
    public static final IRI SUBJECT_OF = vf.createIRI(NAMESPACE, "subjectOf");

    public static final IRI TEACHES = vf.createIRI(NAMESPACE, "teaches");

    public static final IRI KEYWORDS = vf.createIRI(NAMESPACE, "keywords");
    public static final IRI AUTHOR = vf.createIRI(NAMESPACE, "author");
    public static final IRI VALUE = vf.createIRI(NAMESPACE, "value");
    public static final IRI DATASET = vf.createIRI(NAMESPACE, "Dataset");;
    public static final IRI CONTENT_URL = vf.createIRI(NAMESPACE, "contentUrl");
    public static final IRI CONTENT_SIZE = vf.createIRI(NAMESPACE, "contentSize");
    public static final IRI UPLOAD_DATE = vf.createIRI(NAMESPACE, "uploadDate");
    public static final IRI NAME = vf.createIRI(NAMESPACE, "name");
    public static final IRI MEDIA_OBJECT = vf.createIRI(NAMESPACE, "MediaObject");
    public static final IRI DEFINED_TERM = vf.createIRI(NAMESPACE, "DefinedTerm");
    public static final IRI VIDEO_OBJECT = vf.createIRI(NAMESPACE, "VideoObject");
    public static final IRI PROPERTY_VALUE = vf.createIRI(NAMESPACE, "PropertyValue");
    public static final IRI STRUCTURED_VALUE = vf.createIRI(NAMESPACE, "StructuredValue");
    public static final IRI QUANTITATIVE_VALUE = vf.createIRI(NAMESPACE, "QuantitativeValue");
    public static final IRI CREATIVE_WORK = vf.createIRI(NAMESPACE, "CreativeWork");
    public static final IRI HAS_DEFINED_TERM = vf.createIRI(NAMESPACE, "hasDefinedTerm");
    public static final IRI IDENTIFIER = vf.createIRI(NAMESPACE, "identifier");
    public static final IRI TITLE = vf.createIRI(NAMESPACE, "title");
    public static final IRI TERM_CODE = vf.createIRI(NAMESPACE, "termCode");
    public static final IRI CATEGORY_CODE = vf.createIRI(NAMESPACE, "CategoryCode");
    public static final IRI THING = vf.createIRI(NAMESPACE, "Thing");

    public static final IRI ORGANIZATION = vf.createIRI(NAMESPACE, "Organization");

    public static final IRI PERSON = vf.createIRI(NAMESPACE, "Person");

    public static final IRI PLACE = vf.createIRI(NAMESPACE, "Place");
    public static final IRI PRODUCT = vf.createIRI(NAMESPACE, "Product");

    public static final IRI URL = vf.createIRI(NAMESPACE, "url");

    public static Set<IRI> getClassifierTypes() {
        return Set.of(
                DEFINED_TERM,
                CATEGORY_CODE
        );
    }

    public static Set<IRI> getIndividualTypes() {
        return Set.of(
                CREATIVE_WORK,
                THING,
                ORGANIZATION,
                PERSON,
                PLACE,
                PRODUCT

        );
    }

    public static Set<IRI> getCharacteristicProperties() {
        return Set.of(
                IDENTIFIER,
                TITLE,
                NAME,
                TERM_CODE,
                URL
        );
    }
}
