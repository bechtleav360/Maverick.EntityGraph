package cougar.graph.tests.api.v3;

public interface VerificationTest {

    void createEntityWithValidProof();

    void createEntityWithProofInvalidSyntax();

    void createEntityWithProofUnverifyable();


    void readEntityWithProofParameter();
}
