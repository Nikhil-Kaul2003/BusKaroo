package busKaroo.dto.response;

import busKaroo.model.enums.UserRole;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String preferredCurrency,
        String country,
        String avatarData,
        UserRole role) {}
