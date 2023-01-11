package uk.gov.dluhc.registercheckerapi.config

import java.net.URI

data class LocalStackContainerSettings(
    val apiUrl: String,
    val queueUrlInitiateApplicantRegisterCheck: String,
    val queueUrlConfirmRegisterCheckResult: String,
    val queueUrlRemoveRegisterCheckData: String,
) {
    val mappedQueueUrlInitiateApplicantRegisterCheck: String = toMappedUrl(queueUrlInitiateApplicantRegisterCheck, apiUrl)
    val mappedQueueUrlConfirmRegisterCheckResult: String = toMappedUrl(queueUrlConfirmRegisterCheckResult, apiUrl)
    val mappedQueueUrlRemoveRegisterCheckData: String = toMappedUrl(queueUrlRemoveRegisterCheckData, apiUrl)

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
