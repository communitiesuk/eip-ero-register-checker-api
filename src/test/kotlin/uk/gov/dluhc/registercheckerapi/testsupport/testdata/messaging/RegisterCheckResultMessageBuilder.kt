package uk.gov.dluhc.registercheckerapi.testsupport.testdata.messaging

import net.datafaker.providers.base.Address
import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckAddress
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckMatch
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckPersonalDetail
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResult
import uk.gov.dluhc.applicationsapi.messaging.models.RegisterCheckResultMessage
import uk.gov.dluhc.applicationsapi.messaging.models.SourceType
import uk.gov.dluhc.registercheckerapi.database.entity.PersonalDetail
import uk.gov.dluhc.registercheckerapi.dto.AddressDto
import uk.gov.dluhc.registercheckerapi.dto.RegisterCheckMatchDto
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate
import java.util.UUID

fun buildRegisterCheckResultMessage(
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    sourceReference: String = "VPIOKNHPBP",
    sourceCorrelationId: UUID = UUID.randomUUID(),
    registerCheckResult: RegisterCheckResult = RegisterCheckResult.EXACT_MINUS_MATCH,
    matches: List<RegisterCheckMatch> = listOf(buildVcaRegisterCheckMatch())
) = RegisterCheckResultMessage(
    sourceType = sourceType,
    sourceReference = sourceReference,
    sourceCorrelationId = sourceCorrelationId,
    registerCheckResult = registerCheckResult,
    matches = matches
)

fun buildVcaRegisterCheckMatch(
    personalDetail: RegisterCheckPersonalDetail = buildVcaRegisterCheckPersonalDetailSqs(),
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

fun buildVcaRegisterCheckMatchFromMatchApi(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch): RegisterCheckMatch =
    with(match) {
        buildVcaRegisterCheckMatch(
            personalDetail = buildVcaRegisterCheckPersonalDetailSqsFromApiModel(this),
            emsElectoralId = emsElectorId,
            franchiseCode = franchiseCode,
            registeredStartDate = registeredStartDate,
            registeredEndDate = registeredEndDate,
        )
    }

fun buildVcaRegisterCheckMatchFromMatchDto(match: RegisterCheckMatchDto): RegisterCheckMatch =
    with(match) {
        buildVcaRegisterCheckMatch(
            personalDetail = buildVcaRegisterCheckPersonalDetailSqsFromDto(this),
            emsElectoralId = emsElectorId,
            franchiseCode = franchiseCode,
            registeredStartDate = registeredStartDate,
            registeredEndDate = registeredEndDate,
        )
    }

fun buildVcaRegisterCheckPersonalDetailSqs(
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    dateOfBirth: LocalDate? = faker.date().birthday().toLocalDateTime().toLocalDate(),
    email: String? = faker.internet().emailAddress(),
    phone: String? = faker.phoneNumber().cellPhone(),
    address: RegisterCheckAddress = buildVcaRegisterCheckAddressSqs()
) = RegisterCheckPersonalDetail(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    dateOfBirth = dateOfBirth,
    phone = phone,
    email = email,
    address = address
)

private fun buildVcaRegisterCheckAddressSqs(
    fakeAddress: Address = faker.address(),
    property: String? = fakeAddress.buildingNumber(),
    street: String = fakeAddress.streetName(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    postcode: String = fakeAddress.postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12),
) = RegisterCheckAddress(
    property = property,
    street = street,
    locality = locality,
    town = town,
    area = area,
    postcode = postcode,
    uprn = uprn,
)

fun buildVcaRegisterCheckPersonalDetailSqsFromApiModel(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch): RegisterCheckPersonalDetail =
    with(match) {
        buildVcaRegisterCheckPersonalDetailSqs(
            firstName = fn,
            middleNames = mn,
            surname = ln,
            dateOfBirth = dob,
            phone = phone,
            email = email,
            address = buildVcaRegisterCheckAddressSqsFromApiModel(match)
        )
    }

fun buildVcaRegisterCheckPersonalDetailSqsFromDto(match: RegisterCheckMatchDto): RegisterCheckPersonalDetail =
    with(match.personalDetail) {
        buildVcaRegisterCheckPersonalDetailSqs(
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            dateOfBirth = dateOfBirth,
            phone = phone,
            email = email,
            address = buildVcaRegisterCheckAddressSqsFromDto(address)
        )
    }

fun buildVcaRegisterCheckPersonalDetailSqsFromEntity(personalDetailEntity: PersonalDetail): RegisterCheckPersonalDetail =
    with(personalDetailEntity) {
        buildVcaRegisterCheckPersonalDetailSqs(
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            dateOfBirth = dateOfBirth,
            phone = phoneNumber,
            email = email,
            address = buildVcaRegisterCheckAddressSqsFromEntity(address)
        )
    }

private fun buildVcaRegisterCheckAddressSqsFromApiModel(match: uk.gov.dluhc.registercheckerapi.models.RegisterCheckMatch) =
    with(match) {
        buildVcaRegisterCheckAddressSqs(
            property = regproperty,
            street = regstreet,
            locality = reglocality,
            town = regtown,
            area = regarea,
            postcode = regpostcode,
            uprn = reguprn,
        )
    }

private fun buildVcaRegisterCheckAddressSqsFromDto(address: AddressDto): RegisterCheckAddress =
    with(address) {
        buildVcaRegisterCheckAddressSqs(
            property = property,
            street = street,
            locality = locality,
            town = town,
            area = area,
            postcode = postcode,
            uprn = uprn,
        )
    }

private fun buildVcaRegisterCheckAddressSqsFromEntity(address: uk.gov.dluhc.registercheckerapi.database.entity.Address): RegisterCheckAddress =
    with(address) {
        buildVcaRegisterCheckAddressSqs(
            property = property,
            street = street,
            locality = locality,
            town = town,
            area = area,
            postcode = postcode,
            uprn = uprn,
        )
    }

private fun aValidEmsElectoralId() = faker.examplify("AAAAAAA")

private fun aValidFranchiseCode() = faker.examplify("AAA")
