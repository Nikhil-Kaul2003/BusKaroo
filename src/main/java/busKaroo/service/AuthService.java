package busKaroo.service;

import busKaroo.dao.UserRepository;
import busKaroo.dto.mapper.UserMapper;
import busKaroo.dto.request.LoginRequest;
import busKaroo.dto.request.RegisterRequest;
import busKaroo.dto.request.AdminRegisterRequest;
import busKaroo.dto.response.AuthResponse;
import busKaroo.model.User;
import busKaroo.model.enums.UserRole;
import busKaroo.security.JwtTokenProvider;
import busKaroo.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Value("${app.admin-registration-key:}")
    private String adminRegistrationKey;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user =
                userRepository
                        .findByEmail(request.email())
                        .orElseThrow(() -> new IllegalStateException("User not found after auth"));
        return new AuthResponse(jwt, userMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return registerAccount(request, UserRole.user);
    }

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        if (adminRegistrationKey == null || adminRegistrationKey.isBlank()
                || request.registrationKey() == null
                || !MessageDigest.isEqual(
                        adminRegistrationKey.getBytes(StandardCharsets.UTF_8),
                        request.registrationKey().getBytes(StandardCharsets.UTF_8))) {
            throw new ForbiddenException("Administrator registration key is invalid or not configured");
        }
        RegisterRequest account = new RegisterRequest(
                request.firstName(), request.lastName(), request.email(), request.password(),
                request.phone(), "INR", "India", null);
        return registerAccount(account, UserRole.admin);
    }

    private AuthResponse registerAccount(RegisterRequest request, UserRole role) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.phone() != null && !request.phone().isBlank()
                && userRepository.existsByPhone(request.phone().trim())) {
            throw new IllegalArgumentException("Phone number is already registered");
        }

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        
        // Save phone as null if blank to avoid violating unique constraint
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);

        // Indian fares are stored and paid in INR.
        String currency = request.preferredCurrency();
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
        user.setPreferredCurrency(currency);

        // Save country as null if blank or unlisted
        String country = request.country();
        if (country == null || country.isBlank() || "not_in_list".equals(country)) {
            country = null;
        }
        user.setCountry(country);
        user.setAvatarData(request.avatarData());

        userRepository.saveAndFlush(user);

        // Authenticate immediately after register
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return new AuthResponse(jwt, userMapper.toResponse(user));
    }
}
