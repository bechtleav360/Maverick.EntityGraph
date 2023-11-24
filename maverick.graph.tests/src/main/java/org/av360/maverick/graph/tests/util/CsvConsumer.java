package org.av360.maverick.graph.tests.util;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.reactive.server.EntityExchangeResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CsvConsumer implements Consumer<EntityExchangeResult<byte[]>> {
    private String result;

    public Set<Map<String, String>> getRows() {
        return rows;
    }

    Set<Map<String, String>> rows = new HashSet<>();

    StringBuilder dump = new StringBuilder();


    public String getMapAsString() {
        StringBuilder result = new StringBuilder();
        rows.stream().findFirst().ifPresent(hrow -> {
            hrow.keySet().forEach(header -> {

                result.append(StringUtils.rightPad(header, 40, "_")).append(" | ");
            });
            result.append('\n');
        });
        rows.forEach(row -> {
            row.values().forEach(value -> {
                value = StringUtils.abbreviateMiddle(value, "...", 40);
                result.append(StringUtils.rightPad(value, 40)).append(" | ");
            });
            result.append('\n');
        });

        return result.toString();
    }

    public static String cutStringMiddle(String input) {
        int length = input.length();
        if (length <= 40) {
            return input; // Return the original string if it's 40 characters or less
        }

        // Calculate the length of the beginning and end parts
        int partLength = (40 - 3) / 2; // 3 for the length of the ellipses
        String start = input.substring(0, partLength);
        String end = input.substring(length - partLength);

        return start + "..." + end; // Concatenate with ellipses in the middle
    }

    @Override
    public void accept(EntityExchangeResult<byte[]> entityExchangeResult) {
        Assertions.assertNotNull(entityExchangeResult);

        byte[] responseBody = entityExchangeResult.getResponseBodyContent();
        Assertions.assertNotNull(responseBody);


        this.result = new String(responseBody);
        String[] lines = result.split("\\n");
        String[] headers = lines[0].split(",");

        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            Map<String, String> row = new HashMap<>();

            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            rows.add(row);


        }


    }

    public String getAsString() {
        return this.result;
    }
}
