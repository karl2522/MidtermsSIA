package org.google.service;

import java.util.ArrayList;
import java.util.List;

import org.google.config.GooglePeopleApiConfig;
import org.google.model.Contact;
import org.google.model.Contact.EmailEntry;
import org.google.model.Contact.EmailType;
import org.google.model.Contact.PhoneEntry;
import org.google.model.Contact.PhoneType;
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

            // Set emails
            if (contact.getEmails() != null && !contact.getEmails().isEmpty()) {
                List<EmailAddress> emailAddresses = new ArrayList<>();
                for (EmailEntry emailEntry : contact.getEmails()) {
                    EmailAddress emailAddress = new EmailAddress();
                    emailAddress.setValue(emailEntry.getEmail());
                    emailAddress.setType(emailEntry.getType().toString().toLowerCase());
                    emailAddresses.add(emailAddress);
                }
                person.setEmailAddresses(emailAddresses);
            }

            // Set phones
            if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                List<PhoneNumber> phoneNumbers = new ArrayList<>();
                for (PhoneEntry phoneEntry : contact.getPhones()) {
                    PhoneNumber phoneNumber = new PhoneNumber();
                    phoneNumber.setValue(phoneEntry.getNumber());
                    phoneNumber.setType(phoneEntry.getType().toString().toLowerCase());
                    phoneNumbers.add(phoneNumber);
                }
                person.setPhoneNumbers(phoneNumbers);
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
            person.setEtag(contact.getEtag());
            
            // Set name
            Name name = new Name();
            name.setGivenName(contact.getName());
            if (StringUtils.hasText(contact.getFamilyName())) {
                name.setFamilyName(contact.getFamilyName());
            }
            person.setNames(List.of(name));

            // Set emails
            if (contact.getEmails() != null && !contact.getEmails().isEmpty()) {
                List<EmailAddress> emailAddresses = new ArrayList<>();
                for (EmailEntry emailEntry : contact.getEmails()) {
                    EmailAddress emailAddress = new EmailAddress();
                    emailAddress.setValue(emailEntry.getEmail());
                    emailAddress.setType(emailEntry.getType().toString().toLowerCase());
                    emailAddresses.add(emailAddress);
                }
                person.setEmailAddresses(emailAddresses);
            }

            // Set phones
            if (contact.getPhones() != null && !contact.getPhones().isEmpty()) {
                List<PhoneNumber> phoneNumbers = new ArrayList<>();
                for (PhoneEntry phoneEntry : contact.getPhones()) {
                    PhoneNumber phoneNumber = new PhoneNumber();
                    phoneNumber.setValue(phoneEntry.getNumber());
                    phoneNumber.setType(phoneEntry.getType().toString().toLowerCase());
                    phoneNumbers.add(phoneNumber);
                }
                person.setPhoneNumbers(phoneNumbers);
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
            contact.setEtag(person.getEtag());
            
            if (person.getNames() != null && !person.getNames().isEmpty()) {
                Name name = person.getNames().get(0);
                contact.setName(name.getGivenName());
                contact.setGivenName(name.getGivenName());
                contact.setFamilyName(name.getFamilyName());
                contact.setDisplayName(name.getDisplayName());
            }

            // Convert email addresses
            if (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) {
                List<EmailEntry> emails = new ArrayList<>();
                for (EmailAddress emailAddress : person.getEmailAddresses()) {
                    EmailEntry entry = new EmailEntry();
                    entry.setEmail(emailAddress.getValue());
                    String type = emailAddress.getType();
                    if (type != null) {
                        try {
                            entry.setType(EmailType.valueOf(type.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            entry.setType(EmailType.OTHER);
                        }
                    }
                    emails.add(entry);
                }
                contact.setEmails(emails);
            }

            // Convert phone numbers
            if (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty()) {
                List<PhoneEntry> phones = new ArrayList<>();
                for (PhoneNumber phoneNumber : person.getPhoneNumbers()) {
                    PhoneEntry entry = new PhoneEntry();
                    entry.setNumber(phoneNumber.getValue());
                    String type = phoneNumber.getType();
                    if (type != null) {
                        try {
                            entry.setType(PhoneType.valueOf(type.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            entry.setType(PhoneType.OTHER);
                        }
                    }
                    phones.add(entry);
                }
                contact.setPhones(phones);
            }

            return contact;
        } catch (Exception e) {
            log.error("Error converting Person to Contact: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert contact", e);
        }
    }
} 