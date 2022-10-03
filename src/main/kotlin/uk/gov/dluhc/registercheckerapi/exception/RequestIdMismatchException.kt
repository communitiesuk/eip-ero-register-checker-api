package uk.gov.dluhc.registercheckerapi.exception

import java.util.UUID

/**
 * Thrown if requestId in query param mismatches with requestId in POST request payload
 */
class RequestIdMismatchException(requestIdInQueryParam: UUID, requestIdInPayload: UUID) :
    RuntimeException("Request requestId:[$requestIdInQueryParam] does not match with requestid:[$requestIdInPayload] in body payload")
