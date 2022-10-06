package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch as RegisterCheckMatchApi

fun buildRegisterCheckMatch(
    id: UUID = UUID.randomUUID(),
    emsElectorId: String = UUID.randomUUID().toString(),
    attestationCount: Int = 0,
    personalDetail: PersonalDetail = buildPersonalDetail(),
    registeredStartDate: LocalDate? = LocalDate.now(),
    registeredEndDate: LocalDate? = LocalDate.now().plusDays(10),
    applicationCreatedAt: Instant = Instant.now(),
    franchiseCode: String? = ""
) = RegisterCheckMatch(
    id = id,
    emsElectorId = emsElectorId,
    attestationCount = attestationCount,
    personalDetail = personalDetail,
    registeredStartDate = registeredStartDate,
    registeredEndDate = registeredEndDate,
    applicationCreatedAt = applicationCreatedAt,
    franchiseCode = franchiseCode
)

fun buildRegisterCheckMatchEntityFromRegisterCheckMatchApi(
    registerCheckMatchApi: RegisterCheckMatchApi,
) = RegisterCheckMatch(
    emsElectorId = registerCheckMatchApi.emsElectorId,
    attestationCount = registerCheckMatchApi.attestationCount,
    personalDetail = buildPersonalDetail(
        firstName = registerCheckMatchApi.fn,
        middleNames = registerCheckMatchApi.mn,
        surname = registerCheckMatchApi.ln,
        dateOfBirth = registerCheckMatchApi.dob,
        email = registerCheckMatchApi.email,
        phoneNumber = registerCheckMatchApi.phone,
        address = buildAddress(
            street = registerCheckMatchApi.regstreet,
            property = registerCheckMatchApi.regproperty,
            locality = registerCheckMatchApi.reglocality,
            town = registerCheckMatchApi.regtown,
            area = registerCheckMatchApi.regarea,
            postcode = registerCheckMatchApi.regpostcode,
            uprn = registerCheckMatchApi.reguprn
        )
    ),
    registeredStartDate = registerCheckMatchApi.registeredStartDate,
    registeredEndDate = registerCheckMatchApi.registeredEndDate,
    applicationCreatedAt = registerCheckMatchApi.applicationCreatedAt?.toInstant()!!,
    franchiseCode = registerCheckMatchApi.franchiseCode
)
