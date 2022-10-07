package uk.gov.dluhc.registercheckerapi.exception

/**
 * Thrown if match count in the POST body payload mismatches with number of register check match list
 */
class RegisterCheckMatchCountMismatchException(errorMessage: String) : RuntimeException(errorMessage)
