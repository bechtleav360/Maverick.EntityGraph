package org.av360.maverick.graph.feature.navigation.controller.encoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.av360.maverick.graph.model.vocabulary.DC;
import org.av360.maverick.graph.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.SKOSXL;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HtmlWriter implements RDFWriter {


    private final RDFWriter delegate;
    private final OutputStream out;
    private final URI requestURI;


    private void appendLine(String line) {
        try {
            out.write(line.getBytes());
            out.write('\n');
        } catch (IOException e) {
            throw new RDFHandlerException(e);
        }
    }

    public HtmlWriter(RDFWriter writer, OutputStream out, URI requestURI) throws IOException {
        delegate = writer;
        this.out = out;
        this.requestURI = requestURI;


        WriterConfig config = new WriterConfig();
        config.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        delegate.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        delegate.set(BasicWriterSettings.PRETTY_PRINT, true);


    }
    @Override
    public RDFFormat getRDFFormat() {
        return delegate.getRDFFormat();
    }

    @Override
    public RDFWriter setWriterConfig(WriterConfig config) {
        return delegate.setWriterConfig(config);
    }

    @Override
    public WriterConfig getWriterConfig() {
        return delegate.getWriterConfig();
    }

    @Override
    public Collection<RioSetting<?>> getSupportedSettings() {
        return delegate.getSupportedSettings();
    }

    @Override
    public <T> RDFWriter set(RioSetting<T> setting, T value) {
        return delegate.set(setting, value);
    }

    @Override
    public void startRDF() throws RDFHandlerException {
        String header = """
                <!doctype html>
                <html lang="en">
                <head>
                    <title>Maverick Entity Graph Navigation</title>
                    <meta charset="UTF-8">
                    
                    <link rel="stylesheet" href="/style.css"></style>
                </head>
                <body>
                <div class="box">
                    <div id="header_box">
                        <div id="header"></div>
                    </div>
                    <div id="navigation_box">
                        <div id="navigation"></div>
                    </div>
                
                    <div id="content"></div>
                </div>
                    <script id="rdf" type="text/turtle">
                """;

        appendLine(header);
        delegate.startRDF();
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        String footer = """
                    </script>
                    <script id="ns" type="application/json">%s</script>
                    <!-- <script src="https://unpkg.com/vue@3/dist/vue.global.js"></script> -->
                    <script src="/script.js"></script>
                </body>
                </html>
                """.formatted(this.getPrefixes());

        delegate.endRDF();
        appendLine(footer);
    }

    private String printStyles() {
        return this.printFile("style.css");
    }

    private String printScripts() {
        StringWriter sw = new StringWriter();
        sw.append("""
                <script type="text/javascript">
                %s
                </script>
                """.formatted(this.printFile("script.js")));
        sw.append("""
                <script type="text/javascript">
                %s
                </script>
                """.formatted(this.printFile("vue.js")));
        return sw.toString().replace("{{host}}", this.requestURI.getHost().replace(".", "\\."));
    }

    private String printFile(String name) {
        StringWriter sw = new StringWriter();
        try(InputStream is = this.getClass().getClassLoader().getResourceAsStream(name)) {
            IOUtils.copy(is, sw, Charset.defaultCharset());
            return sw.toString();
        } catch (IOException | NullPointerException e) {
            return "";
        }
    }


    private Map<String, String> prefixDeclarations = new HashMap<>();
    @Override
    public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
        prefixDeclarations.put(prefix, uri);
        delegate.handleNamespace(prefix, uri);
    }

    private String getPrefixes() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        this.prefixDeclarations.forEach((k,v) -> {
            ObjectNode decl = objectMapper.createObjectNode();
            decl.put("url", this.getUrlFor(k, v));
            decl.put("external", true);
            root.set(k, decl);
        });
        return root.toString();

    }

    private String getUrlFor(String prefix, String url) {
        return switch (prefix) {
            case "hydra": yield "https://www.hydra-cg.com/spec/latest/core/#";
            case "rdf": yield "https://www.w3.org/TR/rdf-syntax-grammar/";
            case "rdfs": yield "https://www.w3.org/TR/rdf-schema/";
            case "sdo": yield "https://schema.org/";
            case "esco": yield "http://data.europa.eu/esco/model#";
            case SKOSXL.PREFIX: yield SKOSXL.NAMESPACE;
            case SKOS.PREFIX: yield SKOS.NAMESPACE;
            case FOAF.PREFIX: yield FOAF.NAMESPACE;
            case DC.PREFIX: yield DC.NAMESPACE;
            case DCTERMS.PREFIX: yield DCTERMS.NAMESPACE;
            default: yield url;
        };
    }


    @Override
    public void handleStatement(Statement st) throws RDFHandlerException {
        delegate.handleStatement(st);

    }

    @Override
    public void handleComment(String comment) throws RDFHandlerException {
        delegate.handleComment(comment);
    }
}
