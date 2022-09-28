package uk.gov.dluhc.registercheckerapi.rest

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.MediaType
import uk.gov.dluhc.registercheckerapi.config.IntegrationTest
import java.util.*

internal class UpdatePendingRegisterCheckIntegrationTest : IntegrationTest() {

	@ParameterizedTest
	@MethodSource("civicaSampleRequests")
	fun `should process civica requests`(filename: String, requestBody: String) {
		webTestClient.post()
				.uri("/registerchecks/${UUID.randomUUID()}")
				.header(REQUEST_HEADER_NAME, CERT_SERIAL_NUMBER_VALUE)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(requestBody)
				.exchange()
				.expectStatus()
				.isOk
	}

	companion object {
		private const val REQUEST_HEADER_NAME = "client-cert-serial"
		private const val CERT_SERIAL_NUMBER_VALUE = "543219999"

		@JvmStatic
		fun civicaSampleRequests(): List<Arguments> = listOf(
				Arguments.of("Multiple Match.json", """
					{
					    "requestId": "5e881061-57fd-4dc1-935f-8401ebe5758f",
					    "gssCode": "T12345678",
					    "createdAt": null,
					    "registerCheckMatchCount": 2,
					    "registerCheckMatches": [{
					            "emsElectorId": "388473",
					            "registeredStartDate": "2015-10-09",
					            "registeredEndDate": null,
					            "attestationCount": null,
					            "franchiseCode": null,
					            "postalVote": {
					                "postalVoteUntilFurtherNotice": true,
					                "ballotAddressReason": "N/A"
					            },
					            "fn": "Ching",
					            "mn": "Valentino",
					            "ln": "Aspery",
					            "dob": "1996-06-30",
					            "regproperty": "10 Beatrice Street",
					            "regstreet": "Xpress Town",
					            "reglocality": "Xpressshire",
					            "regtown": "",
					            "regarea": "",
					            "regpostcode": "XN2 1BB",
					            "reguprn": "100121116700",
					            "phone": "",
					            "email": ""
					        }, {
					            "emsElectorId": "501658",
					            "registeredStartDate": "2018-02-07",
					            "registeredEndDate": null,
					            "attestationCount": null,
					            "franchiseCode": null,
					            "fn": "Ching",
					            "mn": "Kiu",
					            "ln": "Aspery",
					            "dob": "2000-12-22",
					            "regproperty": "10 Beatrice Street",
					            "regstreet": "Xpress Town",
					            "reglocality": "Xpressshire",
					            "regtown": "",
					            "regarea": "",
					            "regpostcode": "XN2 1BB",
					            "reguprn": "100121116700",
					            "phone": "",
					            "email": "zabair.hussain@civica.co.uk"
					        }
					    ]
					}
				""".trimIndent())
		)
	}
}