package cougar.graph.tests.api.v2;

import org.junit.jupiter.api.Test;

public interface MergeDuplicatesScheduler {
    @Test
    void createEmbeddedEntitiesWithSharedItems();

    @Test
    void createEmbeddedEntitiesWithSharedItemsInSeparateRequests() throws InterruptedException;
}
