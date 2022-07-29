package uk.gov.dluhc.registercheckerapi.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class RegisterCheckerController {

    @GetMapping("/registercheck")
    @PreAuthorize("isAuthenticated()")
    fun getEmsEroIdentifier(@RequestHeader(name = "client-cert-serial") certSerialHeader: String): ResponseEntity<String> =
        ResponseEntity<String>(certSerialHeader, HttpStatus.OK)
}
