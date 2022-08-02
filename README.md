# # eip-ero-register-checker-api
Spring Boot microservice exposing a REST API to update register checker status

## Running the application
Either `./gradlew bootRun` or run the class `RegisterCheckerApiApplication`

### Authentication
Requests are assumed pre-authenticated which carry a header defined by property `REQUEST_HEADER_CLIENT_CERT_SERIAL` that is the authenticated EMS client certificate serial number

### Caching
The responses are cached for a configurable period of time defined by property `CACHE_TIMEOUT` (TBD)

### External Environment Variables
The following environment variables must be set in order to run the application: 
- `REQUEST_HEADER_CLIENT_CERT_SERIAL` is the name of header required in request
- `API_IER_URL` is the base URL of the external IER API service
