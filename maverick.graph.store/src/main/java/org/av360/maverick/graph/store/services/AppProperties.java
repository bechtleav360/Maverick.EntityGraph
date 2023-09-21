package org.av360.maverick.graph.store.services;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// This component binds values from application.properties to object via @ConfigurationProperties

@Data
public class AppProperties {

    public List<Menu> getMenus() {
        return menus;
    }

    private List<Menu> menus = new ArrayList<>();

    @Data
    public static class Menu {
        private String name;
        private String path;
        private String title;

        //getters and setters
    }

}