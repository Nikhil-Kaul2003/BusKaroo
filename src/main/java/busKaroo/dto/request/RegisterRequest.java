package busKaroo.dto.request;

public record RegisterRequest(
        String firstName,
        String lastName,
        String email,
        String password,
        String phone,
        String preferredCurrency,
        String country,
        String avatarData) {}
