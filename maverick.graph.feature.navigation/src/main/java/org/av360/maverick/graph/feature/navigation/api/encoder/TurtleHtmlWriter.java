package org.av360.maverick.graph.feature.navigation.api.encoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TurtleHtmlWriter implements RDFWriter {

    private String javascript = "const lines=document.getElementById(\"rdf\").innerText.split(\"\\n\"),prefix=JSON.parse(document.getElementById(\"ns\").innerText);var pretty=\"\";lines.forEach(e=>{e=e.replace(/[\\u00A0-\\u9999<>\\&]/g,e=>\"&#\"+e.charCodeAt(0)+\";\");let t=e.match(/(.*)(@prefix)\\s([a-z]+):\\s(.*)\\s/);t?pretty+=`${t[1]}<span class=\"ns\">${t[2]} ${t[3]} ${t[4]}</span>`:e.split(\" \").forEach(e=>{let t=e.match(/([a-z]+):([a-zA-Z]{1}[a-z0-9A-Z]+)/),r=e.match(/\\&#60;(http:\\/\\/localhost.*)\\&#62;/);r?pretty+=`<a class=\"external\" target=\"_blank\" href=\"${r[1]}\">${e}</a>`:t?prefix[t[1]]&&prefix[t[1]].external?pretty+=`<a class=\"external\" target=\"_blank\" href=\"${prefix[t[1]].url}#${t[2]}\">${e}</a>`:pretty+=`<a class=\"internal\" href=\"/nav/node?id=${e}\">${e}</a>`:pretty+=e,pretty+=\" \"}),pretty+=\"</br>\"}),document.querySelector(\"#source\").innerHTML=pretty;";

    private final TurtleWriter delegate;
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

    public TurtleHtmlWriter(TurtleWriter turtleWriter, OutputStream out, URI requestURI) {
        delegate = turtleWriter;
        this.out = out;
        this.requestURI = requestURI;
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
                <html lang="en">
                <head>
                    <title>Maverick Entity Graph Navigation</title>
                    <meta charset="UTF-8">
                    <style>"""+this.printStyles()+"""
                    </style>
                </head>
                <body>
                    <pre id="source"></pre>
                    <script id="rdf" type="text/turtle">
                """;

        appendLine(header);
        delegate.startRDF();
    }

    @Override
    public void endRDF() throws RDFHandlerException {
        String footer = """
                    </script>
                    <script id="ns" type="application/json">"""+this.getPrefixes()+"""
                    </script>
                    <script type="text/javascript">"""+this.printScripts()+"""
                    </script>
                </body>
                </html>
                """;

        delegate.endRDF();
        appendLine(footer);
    }

    private String printStyles() {
        return this.printFile("style.css");
    }

    private String printScripts() {
        String s = this.printFile("script.js");



        s = s.replace("{{host}}", this.requestURI.getHost().replace(".", "\\."));
        return s;
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
            case "hydra": yield "https://www.hydra-cg.com/spec/latest/core/";
            case "rdf": yield "https://www.w3.org/TR/rdf-syntax-grammar/";
            case "rdfs": yield "https://www.w3.org/TR/rdf-schema/";
            case "sdo": yield "https://schema.org";
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
