package uk.gov.dluhc.emailnotifications

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.AccountSendingPausedException
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildSendEmailRequest
import uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto.buildSendEmailResponse

@ExtendWith(MockitoExtension::class)
class SesEmailClientTest {
    companion object {
        private val EMAIL_SUBJECT = "Email Subject"
        private val EMAIL_BODY = "<html><body><h1>Email Body</h1><body><html>"

        private const val USER_1 = "user_1@communities.gov.uk"
        private const val USER_2 = "user_2@softwire.com"

        private const val SENDERS_EMAIL_ADDRESS = "sender@email.ierds.uk"
        private const val SES_MESSAGE_ID = "jwndkkpobyredayr-boonxyse-rxdm-ltcw-tasw-shovbwefolvk-fnmzmw"
    }

    private lateinit var sesEmailClient: SesEmailClient

    @Mock
    private lateinit var sesClient: SesClient

    @Test
    fun `successfully send an email to recipients`() {
        // Given
        val recipients = setOf(USER_1)
        val ccRecipients = setOf(USER_2)

        val expectedEmailRequest = buildSendEmailRequest(
            recipients,
            ccRecipients,
            EMAIL_SUBJECT,
            EMAIL_BODY,
            SENDERS_EMAIL_ADDRESS
        )

        val sendEmailResponse = buildSendEmailResponse(SES_MESSAGE_ID)
        given(sesClient.sendEmail(any<SendEmailRequest>())).willReturn(sendEmailResponse)

        // When
        sesEmailClient = buildSesEmailClient()
        sesEmailClient.send(recipients, ccRecipients, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `should throw an exception when the email fails to send due to aws exception`() {
        // Given
        val toRecipients = setOf(USER_1, USER_2)
        val ccRecipients = emptySet<String>()

        val expectedEmailRequest = buildSendEmailRequest(
            toRecipients,
            ccRecipients,
            EMAIL_SUBJECT,
            EMAIL_BODY,
            SENDERS_EMAIL_ADDRESS
        )

        val awsException = AccountSendingPausedException.builder().message("please deposit more money").build()
        given(sesClient.sendEmail(any<SendEmailRequest>())).willThrow(awsException)

        // When
        sesEmailClient = buildSesEmailClient()
        val ex = Assertions.catchThrowableOfType(
            { sesEmailClient.send(toRecipients, ccRecipients, EMAIL_SUBJECT, EMAIL_BODY) },
            EmailNotSentException::class.java
        )

        // Then
        assertThat(ex).isNotNull
        assertThat(ex).hasMessage("Failed to send email to [$toRecipients], cc[[]] with subject[$EMAIL_SUBJECT]")
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `should throw an exception when no recipients nor cc recipients are specified`() {
        // Given
        val toRecipientsEmptySet = emptySet<String>()
        val ccRecipientsEmptySet = emptySet<String>()

        // When
        sesEmailClient = buildSesEmailClient()
        val ex = Assertions.catchThrowableOfType(
            { sesEmailClient.send(toRecipientsEmptySet, ccRecipientsEmptySet, EMAIL_SUBJECT, EMAIL_BODY) },
            EmailNotSentException::class.java
        )

        // Then
        assertThat(ex).isNotNull
        assertThat(ex).hasMessage("Failed to send email due to recipientsToEmail and recipientsToCc being empty")
        verifyNoMoreInteractions(sesClient)
    }

    private fun buildSesEmailClient() = SesEmailClient(sesClient, SENDERS_EMAIL_ADDRESS)
}
