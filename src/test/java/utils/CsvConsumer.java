package utils;

import org.eclipse.rdf4j.query.algebra.Str;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

public class CsvConsumer implements Consumer<EntityExchangeResult<byte[]>> {
    public Set<Map<String, String>> getRows() {
        return rows;
    }

    Set<Map<String, String>> rows = new HashSet<>();


    @Override
    public void accept(EntityExchangeResult<byte[]> entityExchangeResult) {
        Assertions.assertNotNull(entityExchangeResult);
        Assertions.assertNotNull(entityExchangeResult.getResponseBody());

        String result = new String(entityExchangeResult.getResponseBody());
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
