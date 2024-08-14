package uk.gov.dluhc.registercheckerapi.testsupport.testdata.dto

import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse

fun buildSendEmailRequest(
    recipients: Set<String>,
    ccRecipients: Set<String>,
    subject: String,
    emailBody: String,
    sendersEmailAddress: String,
): SendEmailRequest =
    SendEmailRequest.builder()
        .destination(
            Destination.builder()
                .toAddresses(recipients)
                .ccAddresses(ccRecipients)
                .build()
        )
        .message(
            Message.builder()
                .subject(Content.builder().data(subject).build())
                .body(Body.builder().html(Content.builder().data(emailBody).build()).build())
                .build()
        )
        .source(sendersEmailAddress)
        .build()

fun buildSendEmailResponse(messageId: String): SendEmailResponse =
    SendEmailResponse.builder().messageId(messageId).build()
