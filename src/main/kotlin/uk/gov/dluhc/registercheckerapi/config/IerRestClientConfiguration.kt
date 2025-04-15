package uk.gov.dluhc.registercheckerapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.acm19.aws.interceptor.http.AwsRequestSigningApacheV5Interceptor
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import uk.gov.dluhc.logging.rest.CorrelationIdRestTemplateClientHttpRequestInterceptor

/**
 * Configuration class exposing a configured [RestTemplate] suitable for calling IER REST APIs.
 * This uses AWS SDK v2 for signing http requests
 */
@Configuration
class IerRestClientConfiguration(
    @Value("\${api.ier.base.url}") private val ierApiBaseUrl: String,
    @Value("\${api.ier.sts.assume.role}") private val ierStsAssumeRole: String,
    @Value("\${api.ier.sts.assume.role.external-id}") private val ierStsAssumeRoleExternalId: String,
    private val correlationIdRestTemplateClientHttpRequestInterceptor: CorrelationIdRestTemplateClientHttpRequestInterceptor,
) {

    companion object {
        private const val API_GATEWAY_SERVICE_NAME = "execute-api"
        private const val STS_SESSION_NAME = "RegisterChecker_IER_Session"
    }

    @Bean
    fun ierRestClient(
        ierClientHttpRequestFactory: ClientHttpRequestFactory,
        httpMessageConverters: List<HttpMessageConverter<*>>,
    ): RestClient =
        RestClient.builder()
            .baseUrl(ierApiBaseUrl)
            .requestFactory(ierClientHttpRequestFactory)
            .messageConverters { converters: MutableList<HttpMessageConverter<*>> ->
                converters.removeAll { it is MappingJackson2HttpMessageConverter }
                converters.addAll(httpMessageConverters)
            }
            .requestInterceptors { interceptors: MutableList<ClientHttpRequestInterceptor> ->
                interceptors.add(correlationIdRestTemplateClientHttpRequestInterceptor)
            }
            .build()

    @Bean
    fun httpMessageConverters(objectMapper: ObjectMapper): List<HttpMessageConverter<*>> =
        listOf(MappingJackson2HttpMessageConverter(objectMapper))

    @Bean
    fun ierClientHttpRequestFactory(stsClient: StsClient): ClientHttpRequestFactory {
        val ierApiSecurityTokenProvider = StsAssumeRoleCredentialsProvider.builder()
            .refreshRequest(
                AssumeRoleRequest.builder()
                    .roleArn(ierStsAssumeRole)
                    .roleSessionName(STS_SESSION_NAME)
                    .externalId(ierStsAssumeRoleExternalId)
                    .build()
            )
            .stsClient(stsClient)
            .build()

        val httpClient = HttpClients.custom()
            .addRequestInterceptorLast(
                AwsRequestSigningApacheV5Interceptor(
                    API_GATEWAY_SERVICE_NAME,
                    AwsV4HttpSigner.create(),
                    ierApiSecurityTokenProvider,
                    DefaultAwsRegionProviderChain().region,
                )
            )
            .build()

        return HttpComponentsClientHttpRequestFactory(httpClient)
    }

    @Bean
    fun stsClient(): StsClient =
        StsClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(DefaultAwsRegionProviderChain().region)
            .build()
}
