package uk.gov.dluhc.registercheckerapi.client

/**
 * Exception classes used when calling `ero-management-api` is not successful.
 * Allows for communicating the error condition/state back to consuming code within this module without exposing the
 * underlying exception and technology.
 * This abstracts the consuming code from having to deal with, for example, a WebClientResponseException
 */

abstract class ElectoralRegistrationOfficeManagementApiException(message: String) : RuntimeException(message)

class ElectoralRegistrationOfficeNotFoundException(eroId: String) :
    ElectoralRegistrationOfficeManagementApiException("ERO $eroId not found")

class ElectoralRegistrationOfficeGeneralException(message: String) :
    ElectoralRegistrationOfficeManagementApiException(message)
