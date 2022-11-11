package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckMatch
import uk.gov.dluhc.registercheckerapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker
import java.time.LocalDate

fun buildRegisterCheckMatchModel(
    personalDetail: RegisterCheckPersonalDetail = buildRegisterCheckPersonalDetail(),
    emsElectoralId: String = aValidEmsElectoralId(),
    franchiseCode: String = aValidFranchiseCode(),
    registeredStartDate: LocalDate? = LocalDate.now().minusDays(2),
    registeredEndDate: LocalDate? = LocalDate.now().plusDays(2),
) = RegisterCheckMatch(
    personalDetail,
    emsElectorId = emsElectoralId,
    franchiseCode = franchiseCode,
    registeredStartDate = registeredStartDate,
    registeredEndDate = registeredEndDate,
)

fun buildRegisterCheckMatchFromMatchModel(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch): RegisterCheckMatch =
    with(match) {
        buildRegisterCheckMatchModel(
            personalDetail = buildRegisterCheckPersonalDetailFromMatchModel(this),
            emsElectoralId = emsElectorId,
            franchiseCode = franchiseCode,
            registeredStartDate = registeredStartDate,
            registeredEndDate = registeredEndDate,
        )
    }

fun buildRegisterCheckMatchFromMatchDto(match: RegisterCheckMatchDto): RegisterCheckMatch =
    with(match) {
        buildRegisterCheckMatchModel(
            personalDetail = buildRegisterCheckPersonalDetailFromMatchDto(this),
            emsElectoralId = emsElectorId,
            franchiseCode = franchiseCode,
            registeredStartDate = registeredStartDate,
            registeredEndDate = registeredEndDate,
        )
    }

private fun aValidEmsElectoralId() = DataFaker.faker.examplify("AAAAAAA")

private fun aValidFranchiseCode(): String = DataFaker.faker.examplify("AAA")
