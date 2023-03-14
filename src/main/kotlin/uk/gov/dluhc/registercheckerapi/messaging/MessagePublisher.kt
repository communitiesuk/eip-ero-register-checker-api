package uk.gov.dluhc.registercheckerapi.messaging

interface MessagePublisher<T> {
    fun publish(payload: T)
}
