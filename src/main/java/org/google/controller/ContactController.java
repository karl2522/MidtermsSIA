package org.google.controller;

import java.util.List;

import org.google.model.Contact;
import org.google.service.GoogleContactsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/contacts")
public class ContactController {

    @Autowired
    private GoogleContactsService contactsService;

    @GetMapping
    public String listContacts(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @AuthenticationPrincipal OAuth2User principal,
            Model model) {
        try {
            if (principal != null) {
                model.addAttribute("userName", principal.getAttribute("name"));
                model.addAttribute("userEmail", principal.getAttribute("email"));
            }

            List<Contact> contacts = contactsService.getAllContacts(authorizedClient);
            model.addAttribute("contacts", contacts);
            model.addAttribute("contactCount", contacts.size());

            log.info("Retrieved {} contacts for user", contacts.size());
            return "contacts/list";
        } catch (Exception e) {
            log.error("Error listing contacts: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load contacts. Please try again later.");
            return "contacts/list";
        }
    }

    @GetMapping("/new")
    public String showAddContactForm(Model model) {
        model.addAttribute("contact", new Contact());
        model.addAttribute("isEdit", false);
        return "contacts/form";
    }

    @PostMapping
    public String createContact(
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @ModelAttribute Contact contact,
            RedirectAttributes redirectAttributes) {
        try {
            Contact createdContact = contactsService.createContact(authorizedClient, contact);
            redirectAttributes.addFlashAttribute("message", "Contact '" + contact.getName() + "' created successfully!");
            log.info("Created new contact: {}", createdContact.getResourceName());
            return "redirect:/contacts";
        } catch (Exception e) {
            log.error("Error creating contact: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create contact. Please try again.");
            redirectAttributes.addFlashAttribute("contact", contact);
            return "redirect:/contacts/new";
        }
    }

    @GetMapping("/people/{id}/edit")
    public String showEditForm(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            String resourceName = "people/" + id;
            log.info("Loading edit form for contact: {}", resourceName);
            Contact contact = contactsService.getContact(authorizedClient, resourceName);
            
            if (contact == null) {
                log.error("Contact not found with ID: {}", resourceName);
                redirectAttributes.addFlashAttribute("error", "Contact not found. Please try again.");
                return "redirect:/contacts";
            }
            
            model.addAttribute("contact", contact);
            model.addAttribute("isEdit", true);
            return "contacts/form";
        } catch (Exception e) {
            log.error("Error showing edit form for contact {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to load contact. Please try again.");
            return "redirect:/contacts";
        }
    }

    @PutMapping("/people/{id}")
    public String updateContact(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            @ModelAttribute Contact contact,
            RedirectAttributes redirectAttributes) {
        try {
            String resourceName = "people/" + id;
            
            // Get current contact to get its etag
            Contact currentContact = contactsService.getContact(authorizedClient, resourceName);
            contact.setEtag(currentContact.getEtag());
            contact.setResourceName(resourceName);
            
            Contact updatedContact = contactsService.updateContact(authorizedClient, resourceName, contact);
            redirectAttributes.addFlashAttribute("message", "Contact '" + contact.getName() + "' updated successfully!");
            return "redirect:/contacts";
        } catch (Exception e) {
            log.error("Error updating contact {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update contact. Please try again.");
            redirectAttributes.addFlashAttribute("contact", contact);
            return "redirect:/contacts/people/" + id + "/edit";
        }
    }

    @DeleteMapping("/people/{id}")
    public String deleteContact(
            @PathVariable String id,
            @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient,
            RedirectAttributes redirectAttributes) {
        try {
            String resourceName = "people/" + id;
            contactsService.deleteContact(authorizedClient, resourceName);
            redirectAttributes.addFlashAttribute("message", "Contact deleted successfully!");
            return "redirect:/contacts";
        } catch (Exception e) {
            log.error("Error deleting contact {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete contact: " + e.getMessage());
            return "redirect:/contacts";
        }
    }

    @GetMapping("/user")
    public ResponseEntity<String> getUser(@AuthenticationPrincipal OAuth2User principal) {
        return ResponseEntity.ok(principal.getAttribute("name"));
    }
} 