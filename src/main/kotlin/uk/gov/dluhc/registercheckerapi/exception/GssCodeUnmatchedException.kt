package uk.gov.dluhc.registercheckerapi.exception

/**
 * Thrown if gss codes returned from ero-management-api does not match with the gss code from the request.
 */
class GssCodeUnmatchedException(certificateSerial: String, requestGssCode: String) :
    RuntimeException("Request gssCode: [$requestGssCode] does not match with gssCode for certificateSerial: [$certificateSerial]")
