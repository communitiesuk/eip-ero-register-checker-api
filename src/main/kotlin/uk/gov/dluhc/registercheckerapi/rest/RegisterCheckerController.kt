package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RegisterCheckerController {

    @GetMapping("/hello")
    fun getElectoralRegistrationOffice(): String? = "Hello Register Checker Api"
}
