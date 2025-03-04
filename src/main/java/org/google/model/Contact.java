package org.google.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Contact {
    private String resourceName;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    private String phone;
    private String address;
    private String category;

    // Additional fields for Google People API
    private String givenName;
    private String familyName;
    private String displayName;
    private String emailAddress;
    private String phoneNumber;
} 