# # eip-ero-register-checker-api
Spring Boot microservice exposing a REST API to update register checker status

## Running the application
Either `./gradlew bootRun` or run the class `RegisterCheckerApiApplication`

### Authentication
Requests are assumed pre-authenticated which carry a header defined by property `REQUEST_HEADER_CLIENT_CERT_SERIAL` that is the authenticated EMS client certificate serial number

### External Environment Variables
The following environment variables must be set in order to run the application:
* `AWS_ACCESS_KEY_ID` - the AWS access key ID
* `AWS_SECRET_ACCESS_KEY` - the AWS secret access key
* `AWS_REGION` - the AWS region
- `REQUEST_HEADER_CLIENT_CERT_SERIAL` is the name of header required in request
- `API_IER_BASE_URL` - the base URL of the external IER REST API service.
- `API_IER_STS_ASSUME_ROLE` - the IAM role in IER's AWS subscription that should be assumed in order to invoke IER REST API services.
- `SQS_EMS_CIDR_UPDATE_QUEUE_NAME` is the name of the queue for EMS CIDR update notifications from IER
- `SQS_INITIATE_APPLICANT_REGISTER_CHECK_QUEUE_NAME` - the queue name for requesting an automated check to determine if the applicant is on the electoral register

#### MYSQL Configuration
The application requires the following environment variables to connect to Mysql:
* `MYSQL_HOST`
* `MYSQL_PORT`
* `MYSQL_USER`
* `MYSQL_PASSWORD` - only used locally or when running tests

#### Liquibase Configuration
* `LIQUIBASE_CONTEXT` Contexts for liquibase scripts.
  For local setup use ddl.