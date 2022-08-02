package uk.gov.dluhc.registercheckerapi.config

import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import javax.management.timer.Timer

private val logger = KotlinLogging.logger {}

@Configuration
@EnableCaching
@EnableScheduling
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

    @Scheduled(fixedRate = Timer.ONE_HOUR)
    @CacheEvict(value = [CERTIFICATE_SERIAL_CACHE_NAME], allEntries = true)
    fun evictCacheAtInterval() {
        logger.info("Evicting all entries from [$CERTIFICATE_SERIAL_CACHE_NAME] cache")
    }
}
