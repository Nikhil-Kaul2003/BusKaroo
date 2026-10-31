package busKaroo.dto.request;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String phone,
        String preferredCurrency,
        String country,
        String avatarData) {}
