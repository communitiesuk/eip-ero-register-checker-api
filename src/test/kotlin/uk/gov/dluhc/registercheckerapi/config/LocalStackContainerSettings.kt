package uk.gov.dluhc.registercheckerapi.config

import java.net.URI

data class LocalStackContainerSettings(
    val apiUrl: String,
    val queueUrlInitiateApplicantRegisterCheck: String,
    val queueUrlConfirmRegisterCheckResult: String,
    val queueUrlPostalVoteConfirmRegisterCheckResult: String,
    val queueUrlProxyVoteConfirmRegisterCheckResult: String,
    val queueUrlOverseasVoteConfirmRegisterCheckResult: String,
    val queueUrlRemoveRegisterCheckData: String,
    val queueUrlRegisterCheckResultResponse: String
) {
    val mappedQueueUrlInitiateApplicantRegisterCheck: String = toMappedUrl(queueUrlInitiateApplicantRegisterCheck, apiUrl)
    val mappedQueueUrlConfirmRegisterCheckResult: String = toMappedUrl(queueUrlConfirmRegisterCheckResult, apiUrl)
    val mappedQueueUrlPostalVoteConfirmRegisterCheckResult: String = toMappedUrl(queueUrlPostalVoteConfirmRegisterCheckResult, apiUrl)
    val mappedQueueUrlProxyVoteConfirmRegisterCheckResult: String = toMappedUrl(queueUrlProxyVoteConfirmRegisterCheckResult, apiUrl)
    val mappedQueueUrlOverseasVoteConfirmRegisterCheckResult: String = toMappedUrl(queueUrlOverseasVoteConfirmRegisterCheckResult, apiUrl)
    val mappedQueueUrlRegisterCheckResultResponse: String = toMappedUrl(queueUrlRegisterCheckResultResponse, apiUrl)
    val mappedQueueUrlRemoveRegisterCheckData: String = toMappedUrl(queueUrlRemoveRegisterCheckData, apiUrl)
    val sesMessagesUrl = "$apiUrl/_aws/ses"

    private fun toMappedUrl(rawUrlString: String, apiUrlString: String): String {
        val rawUrl = URI.create(rawUrlString)
        val apiUrl = URI.create(apiUrlString)
        return URI(
            rawUrl.scheme,
            rawUrl.userInfo,
            apiUrl.host,
            apiUrl.port,
            rawUrl.path,
            rawUrl.query,
            rawUrl.fragment
        ).toASCIIString()
    }
}
