package io.av360.maverick.graph.tests.generator;

import io.av360.maverick.graph.model.shared.RandomIdentifier;
import org.apache.commons.text.RandomStringGenerator;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import reactor.core.publisher.Mono;

import java.io.StringWriter;
import java.util.Random;

public class GeneratorCommons {
    private static RandomStringGenerator identifierGenerator;

    private static RandomStringGenerator wordGenerator;
    private static ValueFactory valueFactory;

    static {
        identifierGenerator = new RandomStringGenerator.Builder()
                .selectFrom("abcdefghijklmnopqrstuvwxyz0123456789".toCharArray())
                .build();

        wordGenerator = new RandomStringGenerator.Builder()
                .selectFrom("abcdefghijklmnopqrstuvwxyz".toCharArray())
                .build();
        valueFactory = SimpleValueFactory.getInstance();
    }

    public static Namespace generateTestNamespace() {
        return new SimpleNamespace("tst", "http://w3id.org/test");
    }

    public static BNode generateAnomyousSubject() {
        return valueFactory.createBNode();
    }

    public static String generateRandomIdentifier(int length) {
        return identifierGenerator.generate(length);
    }


    public static String generateRandomEntityIdentifier() {
        return identifierGenerator.generate(RandomIdentifier.LENGTH);
    }

    static String generateRandomWords(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int wordLength = new Random().nextInt(10)+3;
            sb.append(wordGenerator.generate(wordLength));
            sb.append(" ");
        }
        return sb.toString();
    }

    static Mono<String> serialize(ModelBuilder builder, RDFFormat format ) {
        StringWriter sw = new StringWriter();
        Rio.write(builder.build(), sw, format);
        return Mono.just(sw.toString());
    }

}
