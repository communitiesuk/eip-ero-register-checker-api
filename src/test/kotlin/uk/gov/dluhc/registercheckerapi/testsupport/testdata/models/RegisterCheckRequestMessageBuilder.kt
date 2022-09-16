package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.messaging.models.InitiateRegisterCheckMessage
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckAddress
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckSourceType
import java.time.LocalDate
import java.util.UUID

fun buildInitiateRegisterCheckMessage(
    sourceType: RegisterCheckSourceType = RegisterCheckSourceType.VOTER_CARD,
    sourceReference: String = "VPIOKNHPBP",
    sourceCorrelationId: UUID = UUID.randomUUID(),
    requestedBy: String = "system",
    gssCode: String = "E123456789",
    personalDetail: RegisterCheckPersonalDetail = buildRegisterCheckPersonalDetail()
) = InitiateRegisterCheckMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    requestedBy = requestedBy,
    gssCode = gssCode,
    personalDetail = personalDetail
)

fun buildRegisterCheckPersonalDetail(
    firstName: String = "John",
    middleNames: String? = "James",
    surname: String = "Smith",
    dateOfBirth: LocalDate? = LocalDate.now(),
    phone: String? = "020 7224 3688",
    email: String? = "info@sherlock-holmes.co.uk",
    address: RegisterCheckAddress = buildRegisterCheckAddress()
) = RegisterCheckPersonalDetail(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
    phone = phone,
    email = email,
    address = address
)

fun buildRegisterCheckAddress(
    property: String? = "221 B",
    street: String = "Baker Street",
    locality: String? = "W",
    town: String? = "Westminster",
    area: String? = "London",
    postcode: String = "NW1 6XE",
    uprn: String? = "1234",
) = RegisterCheckAddress(
    property = property,
    street = street,
    locality = locality,
    town = town,
    area = area,
    postcode = postcode,
    uprn = uprn,
)
