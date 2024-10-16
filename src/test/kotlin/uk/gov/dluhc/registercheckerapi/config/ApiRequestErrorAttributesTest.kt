package uk.gov.dluhc.registercheckerapi.config

import jakarta.servlet.RequestDispatcher
import jakarta.validation.constraints.Size
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindException
import org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST
import org.springframework.web.context.request.ServletWebRequest
import uk.gov.dluhc.registercheckerapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

internal class ApiRequestErrorAttributesTest {

    private lateinit var apiRequestErrorAttributes: ApiRequestErrorAttributes

    @BeforeEach
    fun setup() {
        apiRequestErrorAttributes = ApiRequestErrorAttributes()
    }

    @Test
    fun `should get error response given a non-binding result exception`() {
        // Given
        val exception = IllegalStateException("Some unknown state")

        val httpRequest = MockHttpServletRequest()
        val webRequest = ServletWebRequest(httpRequest).apply {
            setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception, SCOPE_REQUEST)
            setAttribute(RequestDispatcher.ERROR_STATUS_CODE, INTERNAL_SERVER_ERROR.value(), SCOPE_REQUEST)
        }

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val actual = apiRequestErrorAttributes.getErrorResponse(webRequest)

        // Then
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(500)
            .hasError("Internal Server Error")
            .hasMessage("Some unknown state")
            .hasNoValidationErrors()
    }

    @Test
    fun `should get error response given a binding result exception`() {
        // Given
        val exception = BindException(TestBean("a"), "testBean")
        exception.rejectValue("aField", "errorCode", "should be greater than 2 chars in length")

        val httpRequest = MockHttpServletRequest()
        val webRequest = ServletWebRequest(httpRequest).apply {
            setAttribute(RequestDispatcher.ERROR_EXCEPTION, exception, SCOPE_REQUEST)
            setAttribute(RequestDispatcher.ERROR_STATUS_CODE, BAD_REQUEST.value(), SCOPE_REQUEST)
        }

        val earliestExpectedTimeStamp = OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        // When
        val actual = apiRequestErrorAttributes.getErrorResponse(webRequest)

        // Then
        assertThat(actual)
            .hasTimestampNotBefore(earliestExpectedTimeStamp)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessage("Validation failed for object='testBean'. Error count: 1")
            .hasValidationError("Error on field 'aField': rejected value [a], should be greater than 2 chars in length")
    }
}

data class TestBean(
    @Size(min = 2)
    val aField: String
)
