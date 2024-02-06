package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import uk.gov.dluhc.registercheckerapi.dto.VotingArrangementDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate

fun buildVotingArrangementDto(
    untilFurtherNotice: Boolean = false,
    forSingleDate: LocalDate? = faker.date().birthday().toLocalDateTime().toLocalDate(),
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
) = VotingArrangementDto(
    untilFurtherNotice = untilFurtherNotice,
    forSingleDate = forSingleDate,
    startDate = startDate,
    endDate = endDate,
)
