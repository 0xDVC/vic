package com.ecommerce.vic.service;


import com.ecommerce.vic.dto.user.ChangePasswordRequest;
import com.ecommerce.vic.dto.user.UpdateProfileRequest;
import com.ecommerce.vic.dto.user.UserResponse;
import com.ecommerce.vic.exception.InvalidOperationException;
import com.ecommerce.vic.exception.InvalidPasswordException;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.exception.UnauthorizedException;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.repository.UserRepository;
import com.ecommerce.vic.constants.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getCurrentUserProfile() {
        return mapToUserResponse(getCurrentUser());
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        user.setFirstName(request.firstName() != null ? request.firstName() : user.getFirstName());
        user.setLastName(request.lastName() != null ? request.lastName() : user.getLastName());
        user.setPhone(request.phone() != null ? request.phone() : user.getPhone());
        user.setStreetAddress(request.streetAddress() != null ? request.streetAddress() : user.getStreetAddress());
        user.setCity(request.city() != null ? request.city() : user.getCity());
        user.setState(request.state() != null ? request.state() : user.getState());
        user.setPostalCode(request.postalCode() != null ? request.postalCode() : user.getPostalCode());
        user.setCountry(request.country() != null ? request.country() : user.getCountry());

        return mapToUserResponse(userRepository.save(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // Verify current password
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public List<UserResponse> getAllCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER)
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User currentUser = getCurrentUser();
        User requestedUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Users can only access their own profile unless they're admins
        if (!currentUser.getRole().equals(UserRole.ADMIN) && !currentUser.getUserId().equals(id)) {
            throw new UnauthorizedException("Not authorized to access this profile");
        }

        return mapToUserResponse(requestedUser);
    }

    public void deleteUser(Long id) {
        User currentUser = getCurrentUser();

        // Only admins can delete users
        if (!currentUser.getRole().equals(UserRole.ADMIN)) {
            throw new UnauthorizedException("Not authorized to delete users");
        }

        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Prevent deleting the last admin
        if (userToDelete.getRole().equals(UserRole.ADMIN) &&
                userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new InvalidOperationException("Cannot delete the last admin user");
        }

        userRepository.delete(userToDelete);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStreetAddress(),
                user.getCity(),
                user.getState(),
                user.getPostalCode(),
                user.getCountry(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
