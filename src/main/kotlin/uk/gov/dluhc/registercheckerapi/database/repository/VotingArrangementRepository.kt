package uk.gov.dluhc.registercheckerapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.dluhc.registercheckerapi.database.entity.VotingArrangement
import java.util.UUID

interface VotingArrangementRepository : JpaRepository<VotingArrangement, UUID>
