package uk.gov.dluhc.emailnotifications

import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.Content
import software.amazon.awssdk.services.ses.model.Destination
import software.amazon.awssdk.services.ses.model.Message
import software.amazon.awssdk.services.ses.model.SendEmailRequest

fun buildSendEmailRequest(
    sender: String,
    emailToAddresses: Set<String>,
    emailCcAddresses: Set<String>,
    subject: String,
    emailHtmlBody: String,
): SendEmailRequest =
    buildRequest(
        sender = sender,
        emailDestination = buildDestination(emailToAddresses, emailCcAddresses),
        emailMessage = buildMessage(
            emailSubject = buildContent(subject),
            emailBody = buildBody(buildContent(emailHtmlBody))
        )
    )

private fun buildRequest(
    sender: String,
    emailDestination: Destination,
    emailMessage: Message,
) = SendEmailRequest.builder()
    .destination(emailDestination)
    .message(emailMessage)
    .source(sender)
    .build()

private fun buildMessage(
    emailSubject: Content,
    emailBody: Body,
) = Message.builder()
    .subject(emailSubject)
    .body(emailBody)
    .build()

private fun buildBody(emailContent: Content) =
    Body.builder()
        .html(emailContent)
        .build()

private fun buildContent(emailHtmlBody: String) =
    Content.builder()
        .data(emailHtmlBody)
        .build()

private fun buildDestination(recipientsToEmail: Set<String>, recipientsToCc: Set<String>) =
    Destination.builder()
        .toAddresses(recipientsToEmail)
        .ccAddresses(recipientsToCc)
        .build()
