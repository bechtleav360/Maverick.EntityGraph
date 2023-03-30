package io.av360.maverick.graph.tests.generator;

import io.av360.maverick.graph.model.vocabulary.SDO;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import static io.av360.maverick.graph.tests.generator.GeneratorCommons.*;


public class EntitiesGenerator {



    public static Model generateCreativeWork() {

        ModelBuilder builder = new ModelBuilder();
        builder.setNamespace(generateTestNamespace());
        builder.subject(generateAnomyousSubject());
        builder.add(RDF.TYPE, SDO.CREATIVE_WORK);
        builder.add(SDO.IDENTIFIER, generateRandomIdentifier(8));
        builder.add(SDO.TITLE, generateRandomWords(5));

        return builder.build();
    }

    public static Model generateDefinedTerm() {
        ModelBuilder builder = new ModelBuilder();
        builder.setNamespace(generateTestNamespace());
        builder.subject(generateAnomyousSubject());
        builder.add(RDF.TYPE, SDO.DEFINED_TERM);
        builder.add(SDO.TERM_CODE, generateRandomWords(1));

        return builder.build();


    }
}
