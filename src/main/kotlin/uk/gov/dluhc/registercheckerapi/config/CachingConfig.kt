package uk.gov.dluhc.registercheckerapi.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CachingConfig {

    companion object {
        const val CERTIFICATE_SERIAL_CACHE_NAME = "certificateSerialToEroIdCache"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(listOf(ConcurrentMapCache(CERTIFICATE_SERIAL_CACHE_NAME)))
        return cacheManager
    }
}
