package org.av360.maverick.graph.tests.api.v2;

import org.junit.jupiter.api.Test;

public interface Subscriptions {
    @Test
    void createSubscription();

    @Test
    void listSubscription();

    @Test
    void generateKey();

    @Test
    void revokeToken();
}
