package org.av360.maverick.graph.tests.util;

import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CsvConsumer implements Consumer<EntityExchangeResult<byte[]>> {
    public Set<Map<String, String>> getRows() {
        return rows;
    }

    Set<Map<String, String>> rows = new HashSet<>();
    StringBuilder dump = new StringBuilder();

    public String getAsString() {
        StringBuilder result = new StringBuilder();
        rows.stream().findFirst().ifPresent(hrow -> {
            hrow.keySet().forEach(header -> {
                result.append(header).append('\t');
            });
        });
        rows.forEach(row -> {
            row.values().forEach(value -> {
                result.append(value).append('\t');
            });
        });

        return result.toString();
    }

    @Override
    public void accept(EntityExchangeResult<byte[]> entityExchangeResult) {
        Assertions.assertNotNull(entityExchangeResult);

        byte[] responseBody = entityExchangeResult.getResponseBodyContent();
        Assertions.assertNotNull(responseBody);


        String result = new String(responseBody);
        String[] lines = result.split("\\n");
        String[] headers = lines[0].split(",");

        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            Map<String, String> row = new HashMap<>();

            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], values[j]);
            }
            rows.add(row);


        }


    }

}
