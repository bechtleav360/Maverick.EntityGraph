package org.av360.maverick.graph.api;
import org.av360.maverick.graph.api.entities.ListEntities;
import org.av360.maverick.graph.api.entities.formats.CreateEntitiesInJsonLDTests;
import org.av360.maverick.graph.api.entities.formats.CreateEntitiesInTurtleTests;
import org.av360.maverick.graph.api.entities.links.CreateLinksTests;
import org.av360.maverick.graph.api.entities.links.RemoveLinkTests;
import org.av360.maverick.graph.api.entities.values.CreateValuesTest;
import org.av360.maverick.graph.api.entities.values.RemoveValuesTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;


@Suite
@SpringBootTest
@SelectClasses({CreateEntitiesInJsonLDTests.class, CreateEntitiesInTurtleTests.class, CreateLinksTests.class, RemoveLinkTests.class, CreateValuesTest.class, RemoveValuesTest.class, ListEntities.class})
public class TestSuite {
}
