package com.bechtle.cougar.graph.api.converter;

import com.bechtle.cougar.graph.domain.model.wrapper.Incoming;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.util.AbstractRDFInserter;
import org.eclipse.rdf4j.rio.*;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RdfUtils {


    public static Optional<RDFParserFactory> getParserFactory(MimeType mimeType) {
        assert mimeType != null;

        return getAvailableParserFactories()
                .parallelStream()
                .filter(rdfParserFactory -> rdfParserFactory.getRDFFormat().hasMIMEType(mimeType.toString()))
                .findFirst();
    }

    public static Optional<RDFWriterFactory> getWriterFactory(MimeType mimeType) {
        assert mimeType != null;
        return RDFWriterRegistry.getInstance().getAll()
                .parallelStream()
                .filter(rdfWriterFactory -> rdfWriterFactory.getRDFFormat().hasMIMEType(mimeType.toString()))
                .findFirst();
    }

    protected static  Collection<RDFParserFactory> getAvailableParserFactories() {
        return RDFParserRegistry.getInstance().getAll();
    }


    public static List<MimeType> getSupportedMimeTypes() {
        return getAvailableParserFactories().parallelStream()
                .map(rdfParserFactory -> rdfParserFactory.getRDFFormat().getMIMETypes())
                .mapMulti((mimetypes, consumer) ->  { for(String mimetype : mimetypes) consumer.accept(mimetype); })
                .map(mimetypeObj -> MimeType.valueOf(mimetypeObj.toString()))
                .collect(Collectors.toList());
    }

    public static TriplesCollector getTriplesCollector() {
        return new TriplesCollector();
    }

    protected static List<MediaType> getSupportedMediaTypes() {
        return getAvailableParserFactories().parallelStream()
                .map(rdfParserFactory -> rdfParserFactory.getRDFFormat().getMIMETypes())
                .mapMulti((mimetypes, consumer) ->  { for(String mimetype : mimetypes) consumer.accept(mimetype); })
                .map(mimetypeObj -> MediaType.parseMediaType(mimetypeObj.toString()))
                .collect(Collectors.toList());
    }


    public static class TriplesCollector extends AbstractRDFInserter {
        private final Incoming model;

        public TriplesCollector() {
            super(SimpleValueFactory.getInstance());
            this.model = new Incoming();
        }


        public Incoming getModel() {
            return model;
        }

        @Override
        protected void addNamespace(String prefix, String name) throws RDF4JException {
            this.model.getBuilder().setNamespace(prefix, name);
        }

        @Override
        protected void addStatement(Resource subj, IRI pred, Value obj, Resource ctxt) throws RDF4JException {
            this.model.getBuilder().add(subj, pred, obj);

        }
    }

    public static MediaType getMediaType(RDFFormat format) {
        return MediaType.parseMediaType(format.getDefaultMIMEType());
    }
}
