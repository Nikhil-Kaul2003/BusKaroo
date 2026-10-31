package busKaroo.dto.response;

public record DriverResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String licenseNumber,
        Boolean isAvailable) {}
