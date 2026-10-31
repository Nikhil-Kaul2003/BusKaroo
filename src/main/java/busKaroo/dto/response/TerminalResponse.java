package busKaroo.dto.response;

public record TerminalResponse(
    Long id,
    String name,
    String city,
    String country
) {}
