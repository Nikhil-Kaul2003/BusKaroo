package busKaroo.dto.mapper;

import busKaroo.dto.response.UserResponse;
import busKaroo.model.User;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getPreferredCurrency(),
                user.getCountry(),
                user.getAvatarData(),
                user.getRole());
    }
}
