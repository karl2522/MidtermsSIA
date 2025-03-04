# Google Contacts Integration

This Spring Boot application integrates with Google Contacts API to manage contacts.

## Setup

### Google OAuth 2.0 Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select an existing one
3. Enable the People API
4. Create OAuth 2.0 credentials (Web application type)
5. Add the following authorized redirect URI:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```

### Environment Setup

Set up your environment variables with your Google OAuth credentials:

For Windows (PowerShell):
```powershell
$env:GOOGLE_CLIENT_ID=738724944140-ia05kv192u1qgbr6q2uesmacelsgba9o.apps.googleusercontent.com
$env:GOOGLE_CLIENT_SECRET=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
```

For Linux/Mac:
```bash
export GOOGLE_CLIENT_ID="your-client-id"
export GOOGLE_CLIENT_SECRET="your-client-secret"
```

## Running the Application

1. Make sure you have set up your environment variables
2. Run the application using:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Access the application at `http://localhost:8080`

## API Endpoints

- `GET /api/contacts` - Get all contacts
- `POST /api/contacts` - Create a new contact
- `PUT /api/contacts/{resourceName}` - Update a contact
- `DELETE /api/contacts/{resourceName}` - Delete a contact
- `GET /api/contacts/user` - Get current user info

## Example Contact JSON

```json
{
  "givenName": "John",
  "familyName": "Doe",
  "emailAddress": "john.doe@example.com",
  "phoneNumber": "+1234567890"
}
``` 