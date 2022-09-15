package cougar.graph.feature.applications.api.dto;

public class Requests {

    public record RegisterApplicationRequest(String label, boolean persistent) {
    }

    public record CreateApiKeyRequest(String label) {}

}
