package uk.gov.dluhc.registercheckerapi.client

/**
 * Exception classes used when calling `ier-api` is unsuccessful.
 * Allows for communicating the error condition/state back to consuming code within this module without exposing the
 * underlying exception and technology.
 * This abstracts the consuming code from having to deal with, for example, a RestException
 */
abstract class IerApiException(message: String) : RuntimeException(message)

class IerEroNotFoundException(certificateSerial: String) :
    IerApiException("EROCertificateMapping for certificateSerial=[$certificateSerial] not found")

class IerGeneralException(message: String) :
    IerApiException(message)
