package org.av360.maverick.graph.model.enums;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationKeysRegistry {
    private static final Map<String, String> registeredConfigurationKeys = new HashMap<>();

    public static Map<String, String> get() {
        return registeredConfigurationKeys;
    }

    public static void add(String key, String description) {
        registeredConfigurationKeys.put(key, description);
    }



}
