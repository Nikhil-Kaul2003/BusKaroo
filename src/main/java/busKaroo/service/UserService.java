package busKaroo.service;

import busKaroo.dao.UserRepository;
import busKaroo.exception.ForbiddenException;
import busKaroo.exception.CustomerNotFoundException;
import busKaroo.model.User;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepo.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional
    public User update(
            Long id,
            String firstName,
            String lastName,
            String phone,
            String preferredCurrency,
            String country,
            String avatarData,
            Long currentUserId,
            boolean isAdmin) {
        User user = findById(id);

        if (!user.getId().equals(currentUserId) && !isAdmin) {
            throw new ForbiddenException("You can only update your own profile");
        }

        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        
        // Handle empty phone mapping to null to prevent unique constraint failures
        if (phone != null) {
            user.setPhone(phone.isBlank() ? null : phone);
        }
        
        if (preferredCurrency != null) user.setPreferredCurrency(preferredCurrency);
        
        // Handle empty or unlisted country mapping to null
        if (country == null || country.isBlank() || "not_in_list".equals(country)) {
            user.setCountry(null);
        } else {
            user.setCountry(country);
        }
        user.setAvatarData(avatarData);

        return userRepo.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepo.delete(user);
    }
}
