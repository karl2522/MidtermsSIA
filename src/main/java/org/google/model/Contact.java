package org.google.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Contact {
    private String resourceName;
    private String etag;
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private List<EmailEntry> emails = new ArrayList<>();
    private List<PhoneEntry> phones = new ArrayList<>();
    private String address;
    private String category;

    // Additional fields for Google People API
    private String givenName;
    private String familyName;
    private String displayName;

    @Data
    public static class EmailEntry {
        private String email;
        private EmailType type = EmailType.OTHER;
    }

    @Data
    public static class PhoneEntry {
        private String number;
        private PhoneType type = PhoneType.OTHER;
    }

    public enum EmailType {
        HOME,
        WORK,
        OTHER
    }

    public enum PhoneType {
        MOBILE,
        HOME,
        WORK,
        OTHER
    }

    // Helper methods to add entries
    public void addEmail(String email, EmailType type) {
        EmailEntry entry = new EmailEntry();
        entry.setEmail(email);
        entry.setType(type);
        this.emails.add(entry);
    }

    public void addPhone(String number, PhoneType type) {
        PhoneEntry entry = new PhoneEntry();
        entry.setNumber(number);
        entry.setType(type);
        this.phones.add(entry);
    }
} 