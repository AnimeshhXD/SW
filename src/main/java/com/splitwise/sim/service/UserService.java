package com.splitwise.sim.service;

import com.splitwise.sim.dto.auth.SignupRequest;
import com.splitwise.sim.dto.user.UpdateProfileRequest;
import com.splitwise.sim.dto.user.UserProfileResponse;
import com.splitwise.sim.dto.user.UserSearchResponse;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.exception.ResourceAlreadyExistsException;
import com.splitwise.sim.exception.ResourceNotFoundException;
import com.splitwise.sim.repository.UserRepository;
import com.splitwise.sim.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletTransactionRepository transactionRepository;

    @Value("${app.wallet.currency:USD}")
    private String currency;

    @Transactional
    public User createUser(SignupRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public List<User> getAllActiveUsers() {
        return userRepository.findAll().stream()
                .filter(User::getIsActive)
                .collect(Collectors.toList());
    }

    /**
     * NEW: Get user profile with balance and stats
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = getUserById(userId);

        BigDecimal balance = transactionRepository.calculateBalance(userId);
        if (balance == null) balance = BigDecimal.ZERO;

        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .currentBalance(balance)
                .currency(currency)
                .friendCount(user.getFriends().size())
                .groupCount(user.getGroups().size())
                .memberSince(user.getCreatedAt())
                .build();
    }

    /**
     * NEW: Update user profile
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        userRepository.save(user);
        log.info("Updated profile for user: {}", userId);

        return getUserProfile(userId);
    }

    /**
     * NEW: Search users by username, email, or full name
     */
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(String query, Long currentUserId) {
        User currentUser = getUserById(currentUserId);

        return userRepository.searchUsers(query).stream()
                .filter(u -> !u.getId().equals(currentUserId)) // Exclude self
                .map(u -> UserSearchResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .isFriend(currentUser.getFriends().contains(u))
                        .build())
                .collect(Collectors.toList());
    }
}
