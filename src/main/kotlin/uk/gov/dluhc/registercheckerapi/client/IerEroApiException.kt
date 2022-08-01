package uk.gov.dluhc.registercheckerapi.client

/**
 * Exception classes used when calling `ier-management-api` is unsuccessful.
 * Allows for communicating the error condition/state back to consuming code within this module without exposing the
 * underlying exception and technology.
 * This abstracts the consuming code from having to deal with, for example, a WebClientResponseException
 */
abstract class IerManagementApiException(message: String) : RuntimeException(message)

class IerNotFoundException(certificateSerial: String) :
    IerManagementApiException("EROCertificateMapping for $certificateSerial not found")

class IerGeneralException(message: String) :
    IerManagementApiException(message)
