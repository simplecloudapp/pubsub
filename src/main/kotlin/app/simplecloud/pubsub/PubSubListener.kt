package app.simplecloud.pubsub

import com.google.protobuf.Message

fun interface PubSubListener<T: Message> {
    fun handle(message: T)
}