package com.academic.oj.dto;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class RegisterDTO {
    @NotBlank(message = "Username cannot be blank")
    private String username;
    
    @NotBlank(message = "Password cannot be blank")
    private String password;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Phone cannot be blank")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid phone number format")
    private String phone;

    @NotBlank(message = "Verification code cannot be blank")
    @Pattern(regexp = "^\\d{6}$", message = "Verification code must be 6 digits")
    private String verificationCode;
    
    private String nickname;
}

