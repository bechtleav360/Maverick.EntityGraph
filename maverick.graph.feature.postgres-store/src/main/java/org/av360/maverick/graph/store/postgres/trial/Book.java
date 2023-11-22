package org.av360.maverick.graph.store.postgres.trial;/*
 * Copyright (c) 2023.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */


import org.springframework.data.annotation.Id;

public class Book {

    @Id
    private Long id;

    private String title;

    private String author;

    public Book() {}

    public Book(String title, String author) {

        this.title = title;
        this.author = author;
    }

    public Book(Long id, String title, String author) {
        
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public Long getId() {

        return this.id;
    }

    public void setId(Long id) {

        this.id = id;
    }

    public String getTitle() {

        return this.title;
    }

    public String getAuthor() {

        return this.author;
    }
}