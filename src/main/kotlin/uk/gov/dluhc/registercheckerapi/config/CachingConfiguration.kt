package uk.gov.dluhc.registercheckerapi.config

import com.google.common.cache.CacheBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

const val ERO_CERTIFICATE_MAPPING_CACHE = "eroCertificateMappings"

@Configuration
@EnableCaching
class CachingConfiguration : CachingConfigurerSupport() {
    @Value("\${caching.time-to-live}")
    private lateinit var timeToLive: Duration

    @Bean
    @Override
    override fun cacheManager(): CacheManager {
        val cacheManager = object : ConcurrentMapCacheManager() {
            override fun createConcurrentMapCache(name: String): Cache {
                val map = CacheBuilder.newBuilder().expireAfterWrite(timeToLive).build<Any, Any>().asMap()
                return ConcurrentMapCache(name, map, true)
            }
        }

        cacheManager.setCacheNames(listOf(ERO_CERTIFICATE_MAPPING_CACHE))
        return cacheManager
    }
}
