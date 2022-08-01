package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RegisterCheckerController {

    @GetMapping("/registercheck")
    @PreAuthorize("isAuthenticated()")
    fun getEmsEroIdentifier(authentication: Authentication): ResponseEntity<String> {
        return ResponseEntity<String>(authentication.credentials.toString(), HttpStatus.OK)
    }
}
