package uk.gov.dluhc.registercheckerapi.config

import com.fasterxml.jackson.databind.ObjectMapper
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
import software.amazon.awssdk.services.sts.StsClient
import java.net.URI

/**
 * Configuration class exposing beans for the LocalStack (AWS) environment.
 */
@Configuration
class LocalStackContainerConfiguration {

    companion object {
        const val DEFAULT_REGION = "us-east-1"
        const val DEFAULT_PORT = 4566

        val localStackContainer: GenericContainer<*> = getInstance()
        private var container: GenericContainer<*>? = null

        /**
         * Creates and starts LocalStack configured with a basic (empty) SQS service.
         * Returns the container that can subsequently be used for further setup and configuration.
         */
        @Synchronized fun getInstance(): GenericContainer<*> {
            if (container == null) {
                container = GenericContainer(
                    DockerImageName.parse("localstack/localstack:1.1.0")
                ).withEnv(
                    mapOf(
                        "SERVICES" to "sqs, sts",
                        "AWS_DEFAULT_REGION" to DEFAULT_REGION,
                    )
                )
                    .withReuse(true)
                    .withExposedPorts(DEFAULT_PORT)
                    .apply {
                        start()
                    }
            }

            return container!!
        }
    }

    @Bean
    fun awsBasicCredentialsProvider(
        @Value("\${cloud.aws.credentials.access-key}") accessKey: String,
        @Value("\${cloud.aws.credentials.secret-key}") secretKey: String,
    ): AwsCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

    @Bean
    @Primary
    fun localStackStsClient(
        awsCredentialsProvider: AwsCredentialsProvider
    ): StsClient {

        val uri = URI.create("http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}")
        return StsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.EU_WEST_2)
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
        applicationContext: ConfigurableApplicationContext,
        @Value("\${sqs.initiate-applicant-register-check-queue-name}") initiateApplicantRegisterCheckQueueName: String,
        @Value("\${sqs.confirm-applicant-register-check-result-queue-name}") confirmRegisterCheckResultMessageQueueName: String,
        objectMapper: ObjectMapper
    ): LocalStackContainerSettings {
        val queueUrlInitiateApplicantRegisterCheck =
            localStackContainer.createSqsQueue(initiateApplicantRegisterCheckQueueName, objectMapper)
        val queueUrlConfirmRegisterCheckResult =
            localStackContainer.createSqsQueue(confirmRegisterCheckResultMessageQueueName, objectMapper)

        val apiUrl = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}"

        TestPropertyValues.of(
            "cloud.aws.sqs.endpoint=$apiUrl",
        ).applyTo(applicationContext)

        return LocalStackContainerSettings(
            apiUrl = apiUrl,
            queueUrlInitiateApplicantRegisterCheck = queueUrlInitiateApplicantRegisterCheck,
            queueUrlConfirmRegisterCheckResult = queueUrlConfirmRegisterCheckResult,
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
}
