package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.registercheckerapi.service.IerService

@RestController
class RegisterCheckerController(private val ierService: IerService) {

    @GetMapping("/registercheck")
    @PreAuthorize("isAuthenticated()")
    fun getEmsEroIdentifier(authentication: Authentication): String =
        ierService.getEroIdentifierForCertificateSerial(authentication.credentials.toString())
}
