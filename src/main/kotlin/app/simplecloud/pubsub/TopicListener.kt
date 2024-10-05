package app.simplecloud.pubsub

internal data class TopicListener<T : com.google.protobuf.Message>(
    val dataType: Class<T>,
    val listener: PubSubListener<T>
)
