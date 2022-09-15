package uk.gov.dluhc.registercheckerapi.config

import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheInterceptor
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

/**
 * Configuration class exposing a configured [RestTemplate] suitable for calling IER REST APIs.
 * This uses AWS SDK v2 for signing http requests
 */
@Configuration
class IerRestTemplateConfiguration(
    @Value("\${api.ier.base.url}") private val ierApiBaseUrl: String,
    @Value("\${api.ier.sts.assume.role}") private val ierStsAssumeRole: String
) {

    companion object {
        private const val API_GATEWAY_SERVICE_NAME = "execute-api"
        private const val STS_SESSION_NAME = "RegisterChecker_IER_Session"
    }

    @Bean
    fun ierRestTemplate(ierClientHttpRequestFactory: ClientHttpRequestFactory): RestTemplate {
        return RestTemplateBuilder()
            .requestFactory { ierClientHttpRequestFactory }
            .rootUri(ierApiBaseUrl)
            .build()
    }

    @Bean
    fun ierClientHttpRequestFactory(stsClient: StsClient): ClientHttpRequestFactory {
        val ierApiSecurityTokenProvider = StsAssumeRoleCredentialsProvider.builder()
            .refreshRequest(
                AssumeRoleRequest.builder()
                    .roleArn(ierStsAssumeRole)
                    .roleSessionName(STS_SESSION_NAME)
                    .build()
            ).stsClient(stsClient).build()

        val httpClientBuilder = HttpClientBuilder.create()
        httpClientBuilder.addInterceptorLast(
            AwsRequestSigningApacheInterceptor(
                API_GATEWAY_SERVICE_NAME,
                Aws4Signer.create(),
                ierApiSecurityTokenProvider,
                DefaultAwsRegionProviderChain().region
            )
        )

        return HttpComponentsClientHttpRequestFactory().apply {
            httpClient = httpClientBuilder.build()
        }
    }

    @Bean
    fun stsClient(): StsClient =
        StsClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(DefaultAwsRegionProviderChain().region)
            .build()
}
