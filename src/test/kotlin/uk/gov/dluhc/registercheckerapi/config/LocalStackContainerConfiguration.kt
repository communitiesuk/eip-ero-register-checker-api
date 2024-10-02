package uk.gov.dluhc.registercheckerapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.sts.StsClient
import java.net.InetAddress
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Configuration class exposing beans for the LocalStack (AWS) environment.
 */
@Configuration
class LocalStackContainerConfiguration {

    private companion object {
        const val DEFAULT_PORT = 4566
    }

    @Bean
    fun awsBasicCredentialsProvider(
        @Value("\${cloud.aws.credentials.access-key}") accessKey: String,
        @Value("\${cloud.aws.credentials.secret-key}") secretKey: String,
    ): AwsCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    /**
     * Creates and starts LocalStack configured with a basic (empty) STS+SQS service.
     * Returns the container that can subsequently be used for further setup and configuration.
     */
    @Bean
    fun localstackContainer(
        @Value("\${cloud.aws.region.static}") region: String
    ): GenericContainer<*> =
        GenericContainer(
            DockerImageName.parse("localstack/localstack:3.0.2")
        )
            .withEnv(
                mapOf(
                    "SERVICES" to "sqs,sts,ses",
                    "AWS_DEFAULT_REGION" to region,
                )
            )
            .withExposedPorts(DEFAULT_PORT)
            .withReuse(true)
            .withCreateContainerCmdModifier { it.withName("register-checker-api-integration-test-localstack") }
            .apply { start() }

    @Bean
    @Primary
    fun localStackStsClient(
        @Qualifier("localstackContainer") localStackContainer: GenericContainer<*>,
        @Value("\${cloud.aws.region.static}") region: String,
        awsCredentialsProvider: AwsCredentialsProvider
    ): StsClient {
        val uri = URI.create("http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}")
        return StsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(region))
            .endpointOverride(uri)
            .build()
    }

    /**
     * Uses the localstack container to configure the various services.
     *
     * @return a [LocalStackContainerSettings] bean encapsulating the various IDs etc of the configured container and services.
     */
    @Bean
    fun localStackContainerSqsSettings(
        @Qualifier("localstackContainer") localStackContainer: GenericContainer<*>,
        applicationContext: ConfigurableApplicationContext,
        @Value("\${sqs.initiate-applicant-register-check-queue-name}") initiateApplicantRegisterCheckQueueName: String,
        @Value("\${sqs.confirm-applicant-register-check-result-queue-name}") confirmRegisterCheckResultMessageQueueName: String,
        @Value("\${sqs.postal-vote-confirm-applicant-register-check-result-queue-name}") postalVoteConfirmRegisterCheckResultMessageQueueName: String,
        @Value("\${sqs.proxy-vote-confirm-applicant-register-check-result-queue-name}") proxyVoteConfirmRegisterCheckResultMessageQueueName: String,
        @Value("\${sqs.overseas-vote-confirm-applicant-register-check-result-queue-name}") overseasVoteConfirmRegisterCheckResultMessageQueueName: String,
        @Value("\${sqs.remove-applicant-register-check-data-queue-name}") removeRegisterCheckDataMessageQueueName: String,
        objectMapper: ObjectMapper
    ): LocalStackContainerSettings {
        val queueUrlInitiateApplicantRegisterCheck = localStackContainer.createSqsQueue(initiateApplicantRegisterCheckQueueName, objectMapper)
        val queueUrlConfirmRegisterCheckResult = localStackContainer.createSqsQueue(confirmRegisterCheckResultMessageQueueName, objectMapper)
        val queueUrlPostalVoteConfirmRegisterCheckResult = localStackContainer.createSqsQueue(postalVoteConfirmRegisterCheckResultMessageQueueName, objectMapper)
        val queueUrlProxyVoteConfirmRegisterCheckResult = localStackContainer.createSqsQueue(proxyVoteConfirmRegisterCheckResultMessageQueueName, objectMapper)
        val queueUrlOverseasVoteConfirmRegisterCheckResult = localStackContainer.createSqsQueue(overseasVoteConfirmRegisterCheckResultMessageQueueName, objectMapper)
        val queueUrlRemoveRegisterCheckData = localStackContainer.createSqsQueue(removeRegisterCheckDataMessageQueueName, objectMapper)

        val apiUrl = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}"

        TestPropertyValues.of("cloud.aws.sqs.endpoint=$apiUrl").applyTo(applicationContext)

        return LocalStackContainerSettings(
            apiUrl = apiUrl,
            queueUrlInitiateApplicantRegisterCheck = queueUrlInitiateApplicantRegisterCheck,
            queueUrlConfirmRegisterCheckResult = queueUrlConfirmRegisterCheckResult,
            queueUrlPostalVoteConfirmRegisterCheckResult = queueUrlPostalVoteConfirmRegisterCheckResult,
            queueUrlProxyVoteConfirmRegisterCheckResult = queueUrlProxyVoteConfirmRegisterCheckResult,
            queueUrlOverseasVoteConfirmRegisterCheckResult = queueUrlOverseasVoteConfirmRegisterCheckResult,
            queueUrlRemoveRegisterCheckData = queueUrlRemoveRegisterCheckData,
        )
    }

    private fun GenericContainer<*>.createSqsQueue(queueName: String, objectMapper: ObjectMapper): String {
        val execInContainer = execInContainer(
            "awslocal", "sqs", "create-queue", "--queue-name", queueName, "--attributes", "DelaySeconds=1"
        )
        return execInContainer.stdout.let {
            objectMapper.readValue(it, Map::class.java)
        }.let {
            it["QueueUrl"] as String
        }
    }

    fun GenericContainer<*>.getEndpointOverride(): URI? {
        // resolve IP address and use that as the endpoint so that path-style access is automatically used for S3/SES
        val ipAddress = InetAddress.getByName(host).hostAddress
        val mappedPort = getMappedPort(DEFAULT_PORT)
        return URI("http://$ipAddress:$mappedPort")
    }

    @Bean
    @Primary
    fun configureEmailIdentityAndExposeEmailClient(
        @Qualifier("localstackContainer") localStackContainer: GenericContainer<*>,
        @Value("\${cloud.aws.region.static}") region: String,
        awsBasicCredentialsProvider: AwsCredentialsProvider,
        emailClientProperties: EmailClientProperties,
    ): SesClient {
        localStackContainer.verifyEmailIdentity(emailClientProperties.sender)

        return SesClient.builder()
            .region(Region.of(region))
            .credentialsProvider(awsBasicCredentialsProvider)
            .applyMutation { builder -> builder.endpointOverride(localStackContainer.getEndpointOverride()) }
            .build()
    }

    private fun GenericContainer<*>.verifyEmailIdentity(emailAddress: String) {
        val execInContainer = execInContainer(
            "awslocal", "ses", "verify-email-identity", "--email-address", emailAddress
        )
        if (execInContainer.exitCode == 0) {
            logger.info { "verified email identity: $emailAddress" }
        } else {
            logger.error { "failed to create email identity: $emailAddress" }
            logger.error { "failed to create email identity[stdout]: ${execInContainer.stdout}" }
            logger.error { "failed to create email identity[stderr]: ${execInContainer.stderr}" }
        }
    }
}
