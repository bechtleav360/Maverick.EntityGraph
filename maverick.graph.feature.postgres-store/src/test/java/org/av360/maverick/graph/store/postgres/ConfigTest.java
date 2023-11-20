/*
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

package org.av360.maverick.graph.store.postgres;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.store.postgres.trial.Book;
import org.av360.maverick.graph.store.postgres.trial.BookRepository;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@ConfigurationPropertiesScan
@SpringBootTest
@ContextConfiguration(initializers = {ConfigTest.Initializer.class})
@Import(ConfigTest.DbInitializer.class)
@Slf4j
public class ConfigTest {


    @Autowired
    BookRepository bookRepository;
    private static final int POSTGRES_PORT = 5432;

    @ClassRule
    public static PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("foo")
            .withUsername("it_user")
            .withPassword("it_pass")
            .withInitScript("sql/init_postgres.sql");

    static class DbInitializer {
        private static boolean initialized = false;

        @Autowired
        void initializeDb(ConnectionFactory connectionFactory) {
            if (!initialized) {
                ResourceLoader resourceLoader = new DefaultResourceLoader();
                Resource[] scripts = new Resource[] {
                        resourceLoader.getResource("sql/init_postgres.sql")
                };
                new ResourceDatabasePopulator(scripts).populate(connectionFactory).block();
                initialized = true;
            }
        }
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            // r2dbc:tc:postgresql:///databasename?TC_IMAGE_TAG=9.6.8
           String url =  postgres.getHost();
           String jdbc = postgres.getJdbcUrl();

           String r2dbcUrl = "r2dbc:tc:postgresql:///%s?TC_IMAGE_TAG=16.1".formatted(postgres.getDatabaseName());


            TestPropertyValues.of(
                    "spring.r2dbc.url=" + r2dbcUrl,
                    "spring.r2dbc.username=" + postgres.getUsername(),
                    "spring.r2dbc.password=" + postgres.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }

    @Test
    public void insertBooks() {
        Book b1 = new Book("My first book", "Bob Author");
        log.info("Saving book");
        Flux<Book> bookFlux = this.bookRepository.save(b1).thenMany(this.bookRepository.findAll());


        StepVerifier.create(bookFlux).assertNext(book -> {
            log.info("Book got assigned key: {}", book.getId() );
          Assertions.assertNotNull(book);
          Assertions.assertNotNull(book.getId());
        }).verifyComplete();

    }
}
