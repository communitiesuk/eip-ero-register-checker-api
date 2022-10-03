package uk.gov.dluhc.registercheckerapi.exception

import java.util.UUID

/**
 * Thrown if a pending register check for a given correlationId does not exist.
 */
class PendingRegisterCheckNotFoundException(correlationId: UUID) :
    RuntimeException("Pending register check for requestid:[$correlationId] not found")
