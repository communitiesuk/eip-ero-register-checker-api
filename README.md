# eip-ero-register-checker-api
Spring Boot microservice that is :
- Consuming and handling SQS messages related to applicant's register checks.
- Exposing secured REST APIs to allow search and update operations related to applicant's register checks.

# External api dependencies
- Soft-wire IER api 
- [eip-ero-management-api](https://github.com/cabinetoffice/eip-ero-management-api)

## Developer Setup
### Kotlin API Developers

Configure your IDE with the code formatter (ktlint):
```
$ ./gradlew ktlintApplyToIdea
```
This only needs doing once to setup your IDE with the code styles.

#### Running Tests
In order to run the tests successfully, you will first need to set the `LOCALSTACK_API_KEY` environment variable (i.e.
within your .bash_profile or similar). Then run:
```
$ ./gradlew check
```
This will run the tests and ktlint. (Be warned, ktlint will hurt your feelings!)

#### Building docker images
```
$ ./gradlew check bootBuildImage
```
This will build a docker image for the Spring Boot application.

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
- `API_ERO_MANAGEMENT_URL` - the base URL of the [ERO Management REST API service](https://github.com/cabinetoffice/eip-ero-management-api).
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