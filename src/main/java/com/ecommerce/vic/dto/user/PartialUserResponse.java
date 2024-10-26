package com.ecommerce.vic.dto.user;

public record PartialUserResponse (
        Long userId,
        String email,
        String fullName,
        String phone
){
}
