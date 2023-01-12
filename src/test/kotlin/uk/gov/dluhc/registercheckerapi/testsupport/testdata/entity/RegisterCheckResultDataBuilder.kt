package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckResultData
import java.util.UUID
import java.util.UUID.randomUUID

fun buildRegisterCheckResultData(
    correlationId: UUID = randomUUID(),
) = RegisterCheckResultData(
    correlationId = correlationId,
    requestBody = requestBodyJson(correlationId)
)

private fun requestBodyJson(requestId: UUID): String =
    """
    {
      "requestid": "$requestId",
      "gssCode": "E09000021",
      "createdAt": "2022-10-05T10:28:37.3052627+01:00",
      "registerCheckMatches": [],
      "registerCheckMatchCount": 0
    }
    """.trimIndent()
