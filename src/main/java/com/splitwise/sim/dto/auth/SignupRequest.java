package com.splitwise.sim.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class SignupRequest {
    @NotBlank private String username;
    @NotBlank @Email private String email;
    @NotBlank private String password;
    private String fullName;
    private String phoneNumber;
}