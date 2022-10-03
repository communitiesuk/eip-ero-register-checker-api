package uk.gov.dluhc.registercheckerapi.exception

import java.util.UUID

/**
 * Thrown if a pending register check for a given correlationId does not exist.
 */
class RegisterCheckNotFoundException(correlationId: UUID) :
    RuntimeException("Register check for correlationId:[$correlationId] not found")
