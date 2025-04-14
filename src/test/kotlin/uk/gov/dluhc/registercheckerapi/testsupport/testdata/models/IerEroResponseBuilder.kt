package uk.gov.dluhc.registercheckerapi.testsupport.testdata.models

import uk.gov.dluhc.external.ier.models.ERODetails
import uk.gov.dluhc.external.ier.models.LocalAuthorityDetails
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEmailAddress
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroId
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomEroName
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomGssCode
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomIpV4Address
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomLocalAuthorityName
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomPhoneNumber
import uk.gov.dluhc.registercheckerapi.testsupport.getRandomWebsiteAddress
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.DataFaker.Companion.faker

fun buildIerEroDetails(
    eroIdentifier: String = getRandomEroId(),
    gssCode: String = getRandomGssCode(),
    activeClientCertificateSerials: List<String> = listOf(),
    ipAddressCidrs: String? = getRandomIpV4Address(),
    localAuthorities: List<LocalAuthorityDetails> = listOf(buildIerLocalAuthorityDetails(gssCode = gssCode)),
    name: String? = getRandomEroName()
) = ERODetails(
    eroIdentifier = eroIdentifier,
    activeClientCertificateSerials = activeClientCertificateSerials,
    ipAddressCidrs = ipAddressCidrs,
    localAuthorities = localAuthorities,
    name = name
)

fun buildIerEroDetailsList(
    count: Int = 10
) = MutableList(count) { buildIerEroDetails() }

fun buildIerLocalAuthorityDetails(
    gssCode: String = getRandomGssCode(),
    localAuthorityNameEn: String = getRandomLocalAuthorityName(),
    isActive: Boolean = true,
    osPlacesCustodianCode: Int? = null,
    addressLine1En: String = faker.address().buildingNumber(),
    addressLine2En: String = faker.address().streetName(),
    addressLine3En: String? = faker.address().city(),
    addressLine4En: String? = faker.address().state(),
    postcode: String = faker.address().postcode(),
    phoneNumberEn: String = getRandomPhoneNumber(),
    emailAddressEn: String = getRandomEmailAddress(),
    urlEn: String = getRandomWebsiteAddress(),
    certificateLocalAuthorityNameEn: String = getRandomLocalAuthorityName(),
    certificateEmailAddressEn: String? = getRandomEmailAddress(),
    certificateUrlEn: String = getRandomWebsiteAddress(),
    certificateEroNameEn: String? = null,
    localAuthorityNameCy: String? = null,
    addressLine1Cy: String? = null,
    addressLine2Cy: String? = faker.address().streetName(),
    addressLine3Cy: String? = null,
    addressLine4Cy: String? = null,
    phoneNumberCy: String? = getRandomPhoneNumber(),
    emailAddressCy: String? = null,
    urlCy: String? = null,
    certificateLocalAuthorityNameCy: String? = getRandomLocalAuthorityName(),
    certificateEmailAddressCy: String? = getRandomEmailAddress(),
    certificateUrlCy: String? = getRandomWebsiteAddress(),
    certificateEroNameCy: String? = null,
) = LocalAuthorityDetails(
    gssCode = gssCode,
    name = localAuthorityNameEn,
    isActive = isActive,
    osPlacesCustodianCode = osPlacesCustodianCode,
    addressLine1 = addressLine1En,
    addressLine2 = addressLine2En,
    addressLine3 = addressLine3En,
    addressLine4 = addressLine4En,
    postcode = postcode,
    phoneNumber = phoneNumberEn,
    emailAddress = emailAddressEn,
    url = urlEn,
    nameCymraeg = localAuthorityNameCy,
    addressLine1Cymraeg = addressLine1Cy,
    addressLine2Cymraeg = addressLine2Cy,
    addressLine3Cymraeg = addressLine3Cy,
    addressLine4Cymraeg = addressLine4Cy,
    phoneNumberCymraeg = phoneNumberCy,
    emailAddressCymraeg = emailAddressCy,
    urlCymraeg = urlCy,
    nameVac = certificateLocalAuthorityNameEn,
    emailAddressVac = certificateEmailAddressEn,
    urlVac = certificateUrlEn,
    nameVacCymraeg = certificateLocalAuthorityNameCy,
    emailAddressVacCymraeg = certificateEmailAddressCy,
    urlVacCymraeg = certificateUrlCy,
    eroNameVac = certificateEroNameEn,
    eroNameVacCymraeg = certificateEroNameCy,
)
