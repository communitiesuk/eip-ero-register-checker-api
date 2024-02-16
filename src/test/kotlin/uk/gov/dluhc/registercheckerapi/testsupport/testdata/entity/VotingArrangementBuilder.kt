package uk.gov.dluhc.registercheckerapi.testsupport.testdata.entity

import uk.gov.dluhc.registercheckerapi.database.entity.VotingArrangement
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate

fun buildVotingArrangement(
    untilFurtherNotice: Boolean = false,
    forSingleDate: LocalDate? = faker.date().birthday().toLocalDateTime().toLocalDate(),
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
) = VotingArrangement(
    untilFurtherNotice = untilFurtherNotice,
    forSingleDate = forSingleDate,
    startDate = startDate,
    endDate = endDate,
)
