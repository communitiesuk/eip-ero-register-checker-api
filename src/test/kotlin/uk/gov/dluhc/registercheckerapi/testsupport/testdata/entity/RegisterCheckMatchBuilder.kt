package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.database.entity.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.database.entity.VotingArrangement
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch as RegisterCheckMatchApi

fun buildRegisterCheckMatch(
    persisted: Boolean = false,
    emsElectorId: String = UUID.randomUUID().toString(),
    attestationCount: Int = 0,
    personalDetail: PersonalDetail = buildPersonalDetail(),
    registeredStartDate: LocalDate? = LocalDate.now(),
    registeredEndDate: LocalDate? = LocalDate.now().plusDays(10),
    applicationCreatedAt: Instant = Instant.now(),
    franchiseCode: String? = "",
    postalVotingArrangement: VotingArrangement? = buildVotingArrangement(),
    proxyVotingArrangement: VotingArrangement? = null,
) = RegisterCheckMatch(
    id = if (persisted) UUID.randomUUID() else null,
    emsElectorId = emsElectorId,
    attestationCount = attestationCount,
    personalDetail = personalDetail,
    registeredStartDate = registeredStartDate,
    registeredEndDate = registeredEndDate,
    applicationCreatedAt = applicationCreatedAt,
    franchiseCode = franchiseCode,
    postalVotingArrangement = postalVotingArrangement,
    proxyVotingArrangement = proxyVotingArrangement,
)

fun buildRegisterCheckMatchEntityFromRegisterCheckMatchApi(
    registerCheckMatchApi: RegisterCheckMatchApi,
) = with(registerCheckMatchApi) {
    RegisterCheckMatch(
        emsElectorId = emsElectorId,
        attestationCount = attestationCount,
        personalDetail = buildPersonalDetail(
            firstName = fn,
            middleNames = mn,
            surname = ln,
            dateOfBirth = dob,
            email = email,
            phoneNumber = phone,
            address = buildAddress(
                street = regstreet,
                property = regproperty,
                locality = reglocality,
                town = regtown,
                area = regarea,
                postcode = regpostcode,
                uprn = reguprn
            )
        ),
        registeredStartDate = registeredStartDate,
        registeredEndDate = registeredEndDate,
        applicationCreatedAt = applicationCreatedAt?.toInstant()!!,
        franchiseCode = franchiseCode,
        postalVotingArrangement = postalVote?.run {
            buildVotingArrangement(
                untilFurtherNotice = postalVoteUntilFurtherNotice!!,
                forSingleDate = postalVoteForSingleDate,
                startDate = postalVoteStartDate,
                endDate = postalVoteEndDate,
            )
        },
        proxyVotingArrangement = proxyVote?.run {
            buildVotingArrangement(
                untilFurtherNotice = proxyVoteUntilFurtherNotice!!,
                forSingleDate = proxyVoteForSingleDate,
                startDate = proxyVoteStartDate,
                endDate = proxyVoteEndDate,
            )
        },
    )
}

fun RegisterCheckMatch.stripIdsForIntegrationTests() {
    id = null
}
