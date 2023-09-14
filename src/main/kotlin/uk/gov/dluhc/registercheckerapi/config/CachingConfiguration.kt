package uk.gov.dluhc.registercheckerapi.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

const val IER_ELECTORAL_REGISTRATION_OFFICES_CACHE = "ier-eros"

@Configuration
@EnableCaching
class CachingConfiguration {
    @Value("\${caching.time-to-live}")
    private lateinit var timeToLive: Duration

    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager()
            .apply {
                setCaffeine(Caffeine.newBuilder().expireAfterWrite(timeToLive))
                setCacheNames(listOf(IER_ELECTORAL_REGISTRATION_OFFICES_CACHE))
            }
    }
}
