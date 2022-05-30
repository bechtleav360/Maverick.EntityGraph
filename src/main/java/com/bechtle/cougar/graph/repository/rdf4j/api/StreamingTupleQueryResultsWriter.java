package com.bechtle.cougar.graph.repository.rdf4j.api;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;

import java.util.Collection;
import java.util.List;

public class StreamingTupleQueryResultsWriter implements TupleQueryResultWriter {

    private TupleQueryResultWriter writer;
    private final TupleQueryResultWriterFactory factory;

    public StreamingTupleQueryResultsWriter(QueryResultFormat format) {
         factory = TupleQueryResultWriterRegistry.getInstance().get(format).orElseThrow();
    }

    public void updateResultWriter(TupleQueryResultWriter writer) {
        this.writer = writer;
    }


    @Override
    public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
        writer.startQueryResult(bindingNames);
    }

    @Override
    public void handleSolution(BindingSet bindingSet) throws TupleQueryResultHandlerException {
        writer.handleSolution(bindingSet);
    }


    @Override
    public TupleQueryResultFormat getTupleQueryResultFormat() {
        return writer.getTupleQueryResultFormat();
    }

    @Override
    public QueryResultFormat getQueryResultFormat() {
        return writer.getQueryResultFormat();
    }

    @Override
    public void handleNamespace(String s, String s1) throws QueryResultHandlerException {
        writer.handleNamespace(s, s1);
    }

    @Override
    public void startDocument() throws QueryResultHandlerException {
        writer.startDocument();
    }

    @Override
    public void handleStylesheet(String s) throws QueryResultHandlerException {
        writer.handleStylesheet(s);
    }

    @Override
    public void startHeader() throws QueryResultHandlerException {
        writer.startHeader();
    }

    @Override
    public void endHeader() throws QueryResultHandlerException {
        writer.endHeader();
    }

    @Override
    public void setWriterConfig(WriterConfig writerConfig) {
        writer.setWriterConfig(writerConfig);
    }

    @Override
    public WriterConfig getWriterConfig() {
        return writer.getWriterConfig();
    }

    @Override
    public Collection<RioSetting<?>> getSupportedSettings() {
        return writer.getSupportedSettings();
    }

    @Override
    public void handleBoolean(boolean value) throws QueryResultHandlerException {
        writer.handleBoolean(value);
    }

    @Override
    public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
        writer.handleLinks(linkUrls);
    }



    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        writer.endQueryResult();
    }


}
