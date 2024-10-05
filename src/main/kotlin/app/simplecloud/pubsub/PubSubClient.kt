package app.simplecloud.pubsub

import build.buf.gen.simplecloud.pubsub.v1.*
import com.google.protobuf.Any
import io.grpc.*
import io.grpc.stub.StreamObserver
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class PubSubClient(
    private val host: String,
    private val port: Int,
    private val callCredentials: CallCredentials? = null
) {

    private var channel: ManagedChannel = createControllerChannel()
    private var stub: PubSubServiceGrpc.PubSubServiceStub = PubSubServiceGrpc.newStub(channel)
        .withCallCredentials(callCredentials)

    @Volatile
    private var shutdownRequested = false

    @Volatile
    private var lastReconnectAttempt = LocalTime.now().minusSeconds(6)

    @Volatile
    private var isReconnecting = false

    private val topicListeners = mutableMapOf<String, MutableList<TopicListener<*>>>()

    fun <T : com.google.protobuf.Message> subscribe(topic: String, dataType: Class<T>, listener: PubSubListener<T>) {
        topicListeners.getOrPut(topic) { mutableListOf() }.add(TopicListener(dataType, listener))

        if (topicListeners[topic]?.size == 1) {
            subscribeToTopic(topic)
        }
    }

    private fun subscribeToTopic(topic: String) {
        val request = SubscriptionRequest.newBuilder()
            .setTopic(topic)
            .build()

        val responseObserver: StreamObserver<Message> = createResponseObserver(topic)

        try {
            stub.subscribe(request, responseObserver)
        } catch (e: StatusRuntimeException) {
            if (shouldReconnect(e)) {
                attemptReconnect { subscribeToTopic(topic) }
            } else {
                throw e
            }
        }
    }

    private fun createResponseObserver(topic: String): StreamObserver<Message> {
        return object : StreamObserver<Message> {
            override fun onNext(message: Message) {
                if (message.topic != topic) return

                topicListeners[topic]?.forEach { topicListener ->
                    if (topicListener.dataType.name.endsWith(message.messageBody.type)) {
                        val messageData = message.messageBody.messageData.unpack(topicListener.dataType)
                        @Suppress("UNCHECKED_CAST")
                        (topicListener.listener as PubSubListener<com.google.protobuf.Message>).handle(messageData)
                    }
                }
            }

            override fun onError(t: Throwable) {
                if (t !is StatusRuntimeException) {
                    t.printStackTrace()
                }
                if (shouldReconnect(t)) {
                    attemptReconnect { subscribeToTopic(topic) }
                }
            }

            override fun onCompleted() {
            }
        }
    }

    fun <T : com.google.protobuf.Message> unsubscribe(topic: String, listener: PubSubListener<T>) {
        topicListeners[topic]?.removeIf { it.listener == listener }
        if (topicListeners[topic]?.isEmpty() == true) {
            topicListeners.remove(topic)
        }
    }

    fun publish(topic: String, message: com.google.protobuf.Message) {
        val request: PublishRequest = PublishRequest.newBuilder()
            .setTopic(topic)
            .setMessageBody(
                MessageBody.newBuilder()
                    .setType(message.descriptorForType.fullName)
                    .setMessageData(Any.pack(message))
                    .build()
            )
            .build()

        val responseObserver: StreamObserver<PublishResponse> = object : StreamObserver<PublishResponse> {
            override fun onNext(response: PublishResponse) {
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
                if (shouldReconnect(t)) {
                    attemptReconnect { publish(topic, message) }
                }
            }

            override fun onCompleted() {
            }
        }

        try {
            stub.publish(request, responseObserver)
        } catch (e: StatusRuntimeException) {
            if (shouldReconnect(e)) {
                attemptReconnect { publish(topic, message) }
            } else {
                throw e
            }
        }
    }

    private fun createControllerChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

    private fun shouldReconnect(t: Throwable): Boolean {
        return t is StatusRuntimeException &&
                (t.status.code == Status.Code.UNAVAILABLE || t.status.code == Status.Code.UNKNOWN)
    }

    private fun attemptReconnect(action: () -> Unit) {
        if (isReconnecting) {
            return
        }
        isReconnecting = true

        thread {
            while (!shutdownRequested) {
                if (attemptSingleReconnect(action)) break

                Thread.sleep(2000)
            }
        }
    }

    private fun attemptSingleReconnect(action: () -> Unit): Boolean {
        if (lastReconnectAttempt.plusSeconds(5).isAfter(LocalTime.now())) {
            return false
        }

        lastReconnectAttempt = LocalTime.now()
        try {
            channel.shutdownNow()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        } catch (ignored: InterruptedException) {
        } catch (ignored: StatusRuntimeException) {
        }

        try {
            channel = createControllerChannel()
            stub = PubSubServiceGrpc.newStub(channel)
                .withCallCredentials(callCredentials)

            action()
            isReconnecting = false
            return true
        } catch (e: StatusRuntimeException) {
            if (!shouldReconnect(e)) {
                throw e
            }
        }
        return false
    }

    fun shutdown() {
        shutdownRequested = true
        topicListeners.clear()
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
