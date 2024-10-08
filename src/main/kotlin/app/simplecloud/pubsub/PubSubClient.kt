package app.simplecloud.pubsub

import build.buf.gen.simplecloud.pubsub.v1.*
import com.google.protobuf.Any
import io.grpc.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class PubSubClient(
    private val host: String,
    private val port: Int,
    private val callCredentials: CallCredentials? = null
) {

    private var channel: ManagedChannel = createControllerChannel()
    private var stub = PubSubServiceGrpcKt.PubSubServiceCoroutineStub(channel)
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
        val request = subscriptionRequest {
            this.topic = topic
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messageFlow = stub.subscribe(request)
                messageFlow.collect {
                    if (it.topic != topic) return@collect

                    topicListeners[topic]?.forEach { topicListener ->
                        if (topicListener.dataType.name.endsWith(it.messageBody.type)) {
                            val messageData = it.messageBody.messageData.unpack(topicListener.dataType)
                            @Suppress("UNCHECKED_CAST")
                            (topicListener.listener as PubSubListener<com.google.protobuf.Message>).handle(messageData)
                        }
                    }
                }

                messageFlow.catch {
                    if (it !is StatusException) {
                        it.printStackTrace()
                    }

                    if (shouldReconnect(it)) {
                        attemptReconnect { subscribeToTopic(topic) }
                    }
                }
            } catch (e: StatusException) {
                if (shouldReconnect(e)) {
                    attemptReconnect { subscribeToTopic(topic) }
                } else {
                    throw e
                }
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
        val request = publishRequest {
            this.topic = topic
            this.messageBody = messageBody {
                this.type = message.descriptorForType.fullName
                this.messageData = Any.pack(message)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                stub.publish(request)
            } catch (e: StatusException) {
                if (shouldReconnect(e)) {
                    attemptReconnect { publish(topic, message) }
                } else {
                    throw e
                }
            }
        }
    }

    private fun createControllerChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }

    private fun shouldReconnect(t: Throwable): Boolean {
        return t is StatusException &&
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
            stub = PubSubServiceGrpcKt.PubSubServiceCoroutineStub(channel)
                .withCallCredentials(callCredentials)

            action()
            isReconnecting = false
            return true
        } catch (e: StatusException) {
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
