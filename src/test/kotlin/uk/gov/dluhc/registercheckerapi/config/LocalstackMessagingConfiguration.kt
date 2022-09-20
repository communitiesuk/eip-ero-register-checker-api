package uk.gov.dluhc.registercheckerapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class LocalstackMessagingConfiguration {
    @Bean
    @Primary
    fun testSqsClient(awsBasicCredentialsProvider: AwsCredentialsProvider, localStackContainerSqsSettings: LocalStackContainerSettings): SqsClient =
        SqsClient.builder()
            .credentialsProvider(awsBasicCredentialsProvider)
            .endpointOverride(URI(localStackContainerSqsSettings.apiUrl))
            .build()
}
