package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RegisterCheckerController {

    companion object {
        const val ERO_ADMIN_GROUP_PREFIX = "ero-admin-"
    }

    /**
     * Service that returns a single Electoral Registration Office by its ID.
     *
     * This endpoint is publicly available and requires no bearer token to access.
     */
    @GetMapping("/hello")
    fun getElectoralRegistrationOffice(): String? = "Hello Register Checker Api"
}
