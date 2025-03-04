package org.google.service;

import java.util.ArrayList;
import java.util.List;

import org.google.config.GooglePeopleApiConfig;
import org.google.model.Contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleContactsService {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private GooglePeopleApiConfig googlePeopleApiConfig;

    public List<Contact> getAllContacts(OAuth2AuthorizedClient authorizedClient) {
        try {
            log.info("Fetching all contacts for user");
            PeopleService peopleService = googlePeopleApiConfig.getPeopleService(authorizedClient);
            
            List<Contact> contacts = new ArrayList<>();
            String pageToken = null;
            
            do {
                ListConnectionsResponse response = peopleService.people().connections()
                    .list("people/me")
                    .setPersonFields("names,emailAddresses,phoneNumbers")
                    .setPageSize(1000) // Maximum allowed by the API
                    .setPageToken(pageToken)
                    .execute();
                
                if (response.getConnections() != null) {
                    for (Person person : response.getConnections()) {
                        contacts.add(convertToContact(person));
                    }
                }
                
                pageToken = response.getNextPageToken();
            } while (pageToken != null);
            
            log.info("Successfully fetched {} contacts", contacts.size());
            return contacts;
        } catch (Exception e) {
            log.error("Error fetching contacts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch contacts", e);
        }
    }

    public Contact createContact(OAuth2AuthorizedClient authorizedClient, Contact contact) {
        try {
            log.info("Creating new contact: {}", contact.getName());
            PeopleService peopleService = googlePeopleApiConfig.getPeopleService(authorizedClient);

            Person person = new Person();
            
            // Set name
            Name name = new Name();
            name.setGivenName(contact.getName());
            if (StringUtils.hasText(contact.getFamilyName())) {
                name.setFamilyName(contact.getFamilyName());
            }
            person.setNames(List.of(name));

            // Set email
            if (StringUtils.hasText(contact.getEmail())) {
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setValue(contact.getEmail());
                person.setEmailAddresses(List.of(emailAddress));
            }

            // Set phone
            if (StringUtils.hasText(contact.getPhone())) {
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.setValue(contact.getPhone());
                person.setPhoneNumbers(List.of(phoneNumber));
            }

            Person createdPerson = peopleService.people()
                .createContact(person)
                .execute();

            log.info("Successfully created contact: {}", createdPerson.getResourceName());
            return convertToContact(createdPerson);
        } catch (Exception e) {
            log.error("Error creating contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create contact", e);
        }
    }

    public Contact updateContact(OAuth2AuthorizedClient authorizedClient, String resourceName, Contact contact) {
        try {
            log.info("Updating contact: {}", resourceName);
            PeopleService peopleService = googlePeopleApiConfig.getPeopleService(authorizedClient);

            Person person = new Person();
            
            // Set name
            Name name = new Name();
            name.setGivenName(contact.getName());
            if (StringUtils.hasText(contact.getFamilyName())) {
                name.setFamilyName(contact.getFamilyName());
            }
            person.setNames(List.of(name));

            // Set email
            if (StringUtils.hasText(contact.getEmail())) {
                EmailAddress emailAddress = new EmailAddress();
                emailAddress.setValue(contact.getEmail());
                person.setEmailAddresses(List.of(emailAddress));
            }

            // Set phone
            if (StringUtils.hasText(contact.getPhone())) {
                PhoneNumber phoneNumber = new PhoneNumber();
                phoneNumber.setValue(contact.getPhone());
                person.setPhoneNumbers(List.of(phoneNumber));
            }

            Person updatedPerson = peopleService.people()
                .updateContact(resourceName, person)
                .setUpdatePersonFields("names,emailAddresses,phoneNumbers")
                .execute();

            log.info("Successfully updated contact: {}", updatedPerson.getResourceName());
            return convertToContact(updatedPerson);
        } catch (Exception e) {
            log.error("Error updating contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update contact", e);
        }
    }

    public void deleteContact(OAuth2AuthorizedClient authorizedClient, String resourceName) {
        try {
            log.info("Deleting contact: {}", resourceName);
            PeopleService peopleService = googlePeopleApiConfig.getPeopleService(authorizedClient);

            peopleService.people()
                .deleteContact(resourceName)
                .execute();

            log.info("Successfully deleted contact: {}", resourceName);
        } catch (Exception e) {
            log.error("Error deleting contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete contact", e);
        }
    }

    public Contact getContact(OAuth2AuthorizedClient authorizedClient, String resourceName) {
        try {
            log.info("Fetching contact with resourceName: {}", resourceName);
            if (resourceName == null || resourceName.isEmpty()) {
                log.error("Invalid resourceName provided: {}", resourceName);
                throw new IllegalArgumentException("Resource name cannot be null or empty");
            }
            
            PeopleService peopleService = googlePeopleApiConfig.getPeopleService(authorizedClient);
            
            Person person = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();
            
            if (person == null) {
                log.error("No person found with resourceName: {}", resourceName);
                return null;
            }
                
            log.info("Successfully fetched contact: {}", resourceName);
            Contact contact = convertToContact(person);
            log.info("Converted contact: {}", contact);
            return contact;
        } catch (Exception e) {
            log.error("Error fetching contact with resourceName {}: {}", resourceName, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch contact", e);
        }
    }

    private Contact convertToContact(Person person) {
        try {
            Contact contact = new Contact();
            contact.setResourceName(person.getResourceName());
            
            if (person.getNames() != null && !person.getNames().isEmpty()) {
                Name name = person.getNames().get(0);
                contact.setName(name.getGivenName());
                contact.setGivenName(name.getGivenName());
                contact.setFamilyName(name.getFamilyName());
                contact.setDisplayName(name.getDisplayName());
            }

            if (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) {
                String email = person.getEmailAddresses().get(0).getValue();
                contact.setEmail(email);
                contact.setEmailAddress(email);
            }

            if (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty()) {
                String phone = person.getPhoneNumbers().get(0).getValue();
                contact.setPhone(phone);
                contact.setPhoneNumber(phone);
            }

            return contact;
        } catch (Exception e) {
            log.error("Error converting Person to Contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert contact", e);
        }
    }
} 