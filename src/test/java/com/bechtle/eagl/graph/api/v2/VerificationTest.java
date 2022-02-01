package com.bechtle.eagl.graph.api.v2;

public interface VerificationTest {

    void createEntityWithValidProof();

    void createEntityWithProofInvalidSyntax();

    void createEntityWithProofUnverifyable();


    void readEntityWithProofParameter();
}
