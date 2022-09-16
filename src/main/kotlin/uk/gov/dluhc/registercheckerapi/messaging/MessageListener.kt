package uk.gov.dluhc.registercheckerapi.messaging

interface MessageListener<PAYLOAD> {
    fun handleMessage(payload: PAYLOAD)
}
