package uk.gov.dluhc.registercheckerapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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

    private companion object {
        const val DEFAULT_PORT = 4566
        const val DEFAULT_ACCESS_KEY_ID = "test"
        const val DEFAULT_SECRET_KEY = "test"
    }

    @Bean
    fun awsBasicCredentialsProvider(): AwsCredentialsProvider =
        StaticCredentialsProvider.create(AwsBasicCredentials.create(DEFAULT_ACCESS_KEY_ID, DEFAULT_SECRET_KEY))

    /**
     * Creates and starts LocalStack configured with a basic (empty) STS service.
     * Returns the container that can subsequently be used for further setup and configuration.
     */
    @Bean
    fun localstackContainer(
        @Value("\${cloud.aws.region.static}") region: String,
        @Value("\${localstack.api.key}") localStackApiKey: String
    ): GenericContainer<*> {
        return GenericContainer(
            DockerImageName.parse("localstack/localstack:latest")
        ).withEnv(
            mapOf(
                "SERVICES" to "sts",
                "AWS_DEFAULT_REGION" to region,
                "LOCALSTACK_API_KEY" to localStackApiKey
            )
        ).withExposedPorts(DEFAULT_PORT)
            .apply {
                start()
            }
    }

    @Bean
    @Primary
    fun localStackStsClient(
        @Qualifier("localstackContainer") localStackContainer: GenericContainer<*>,
        awsCredentialsProvider: AwsCredentialsProvider
    ): StsClient {

        val uri = URI.create("http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}")
        return StsClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.EU_WEST_2)
            .endpointOverride(uri)
            .build()
    }
}
