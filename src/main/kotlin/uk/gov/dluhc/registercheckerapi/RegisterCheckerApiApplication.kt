package uk.gov.dluhc.registercheckerapi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot application bootstrapping class.
 */
@SpringBootApplication
class RegisterCheckerApiApplication

fun main(args: Array<String>) {
    runApplication<RegisterCheckerApiApplication>(*args)
}
