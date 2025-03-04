package org.google.config;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;

@Configuration
public class GooglePeopleApiConfig {

    private static final String APPLICATION_NAME = "Google Contacts Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Bean
    public JsonFactory jsonFactory() {
        return JSON_FACTORY;
    }

    @Bean
    public PeopleService.Builder peopleServiceBuilder() throws GeneralSecurityException, IOException {
        return new PeopleService.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JSON_FACTORY,
            null
        ).setApplicationName(APPLICATION_NAME);
    }

    public PeopleService getPeopleService(OAuth2AuthorizedClient authorizedClient) throws GeneralSecurityException, IOException {
        return peopleServiceBuilder()
            .setHttpRequestInitializer(request -> 
                request.getHeaders().setAuthorization("Bearer " + authorizedClient.getAccessToken().getTokenValue()))
            .build();
    }
} 