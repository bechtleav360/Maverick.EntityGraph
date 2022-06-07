package com.bechtle.cougar.graph.api.v3;

public interface VerificationTest {

    void createEntityWithValidProof();

    void createEntityWithProofInvalidSyntax();

    void createEntityWithProofUnverifyable();


    void readEntityWithProofParameter();
}
