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

#### AWS CodeArtifact Access Set Up

To access libraries stored in the AWS CodeArtifact repository an access token is required that the build script fetches
in the background using the credentials for the `code-artifact` profile. To create this profile on your developer
machine follow these instructions:

```shell
aws configure --profile code-artifact
```

At the prompts configure the `code-artifact` profile as follows:
* Your AWS Access Key ID
* Your AWS Secret Access Key
* Default region name, `eu-west-2`
* Default output format, `json`

Note: AWS CLI must be installed on the developer workstation as a pre-requisite.

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
1. `AWS_ACCESS_KEY_ID` - the AWS access key ID
2. `AWS_SECRET_ACCESS_KEY` - the AWS secret access key
3. `AWS_REGION` - the AWS region
4. `REQUEST_HEADER_CLIENT_CERT_SERIAL` is the name of header required in request
5. `API_ERO_MANAGEMENT_URL` - the base URL of the [ERO Management REST API service](https://github.com/cabinetoffice/eip-ero-management-api).
6. `API_IER_BASE_URL` - the base URL of the external IER REST API service.
7. `API_IER_STS_ASSUME_ROLE` - the IAM role in IER's AWS subscription that should be assumed in order to invoke IER REST API services.
8. `SQS_EMS_CIDR_UPDATE_QUEUE_NAME` is the name of the queue for EMS CIDR update notifications from IER
9. `SQS_INITIATE_APPLICANT_REGISTER_CHECK_QUEUE_NAME` - the queue name for requesting an automated check to determine if the applicant is on the electoral register
10. `SQS_CONFIRM_APPLICANT_REGISTER_CHECK_RESULT_QUEUE_NAME` - the queue name for responding with the result of the register check
11. `SQS_REMOVE_APPLICANT_REGISTER_CHECK_DATA_QUEUE_NAME` - the queue name for removing an applicant's register check data
12. `SQS_POSTAL_VOTE_CONFIRM_APPLICANT_REGISTER_CHECK_RESULT_QUEUE_NAME` - the queue name for responding with the result of the postal vote register check
13. `SQS_PROXY_VOTE_CONFIRM_APPLICANT_REGISTER_CHECK_RESULT_QUEUE_NAME` - the queue name for responding with the result of the proxy vote register check
14. `SQS_OVERSEAS_VOTE_CONFIRM_APPLICANT_REGISTER_CHECK_RESULT_QUEUE_NAME` - the queue name for responding with the result of the overseas vote register check

#### MYSQL Configuration
The application requires the following environment variables to connect to Mysql:
* `MYSQL_HOST`
* `MYSQL_PORT`
* `MYSQL_USER`
* `MYSQL_PASSWORD` - only used locally or when running tests

#### Liquibase Configuration
* `LIQUIBASE_CONTEXT` Contexts for liquibase scripts.
  For local setup use ddl.
