package com.ecommerce.vic.mapper;

import com.ecommerce.vic.dto.user.PartialUserResponse;
import com.ecommerce.vic.dto.user.UserResponse;
import com.ecommerce.vic.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getStreetAddress(),
                user.getCity(),
                user.getState(),
                user.getCountry(),
                user.getPostalCode(),
                user.getRole(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
    public PartialUserResponse toPartialUserResponse(User user) {
        return new PartialUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                user.getPhone()
        );
    }
}
