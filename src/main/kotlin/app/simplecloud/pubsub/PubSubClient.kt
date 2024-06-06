package app.simplecloud.pubsub

import build.buf.gen.simplecloud.pubsub.v1.*
import com.google.protobuf.Any
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import io.grpc.StatusRuntimeException
import io.grpc.Status
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class PubSubClient {

    private var channel: ManagedChannel = createControllerChannel()
    private var stub: PubSubServiceGrpc.PubSubServiceStub = PubSubServiceGrpc.newStub(channel)

    @Volatile
    private var shutdownRequested = false

    fun <T: com.google.protobuf.Message> subscribe(topic: String, dataType: Class<T>, listener: PubSubListener<T>) {
        val request = SubscriptionRequest.newBuilder()
            .setTopic(topic)
            .build()

        val responseObserver: StreamObserver<Message> = object : StreamObserver<Message> {
            override fun onNext(message: Message) {
                if (message.topic != topic) return
                if (!dataType.name.endsWith(message.messageBody.type)) return

                println("Received message from ${message.messageBody.type} ${dataType.name} $topic")
                val messageData = message.messageBody.messageData.unpack(dataType)
                listener.handle(messageData)
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
                if (shouldReconnect(t)) {
                    attemptReconnect { subscribe(topic, dataType, listener) }
                }
            }

            override fun onCompleted() {
                println("Stream completed")
            }
        }

        try {
            stub.subscribe(request, responseObserver)
        } catch (e: StatusRuntimeException) {
            if (shouldReconnect(e)) {
                attemptReconnect { subscribe(topic, dataType, listener) }
            } else {
                throw e
            }
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
                if (response.getSuccess()) {
                    println("Message published successfully!")
                } else {
                    println("Failed to publish message.")
                }
            }

            override fun onError(t: Throwable) {
                t.printStackTrace()
                if (shouldReconnect(t)) {
                    attemptReconnect { publish(topic, message) }
                }
            }

            override fun onCompleted() {
                println("Publish stream completed.")
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
        val port = System.getenv("GRPC_PUBSUB_PORT")?.toInt() ?: 5816
        val host = System.getenv("GRPC_PUBSUB_HOST") ?: "localhost"
        return ManagedChannelBuilder.forAddress(host, port).usePlaintext()
            .build()
    }

    private fun shouldReconnect(t: Throwable): Boolean {
        return t is StatusRuntimeException &&
                (t.status.code == Status.Code.UNAVAILABLE || t.status.code == Status.Code.UNKNOWN)
    }

    private fun attemptReconnect(action: () -> Unit) {
        thread {
            while (!shutdownRequested) {
                channel.shutdownNow()
                try {
                    channel.awaitTermination(5, TimeUnit.SECONDS)
                } catch (ignored: InterruptedException) {
                }

                try {
                    channel = createControllerChannel()
                    stub = PubSubServiceGrpc.newStub(channel)

                    action()
                    break
                } catch (e: StatusRuntimeException) {
                    if (!shouldReconnect(e)) {
                        throw e
                    }
                }

                Thread.sleep(2000)
            }
        }
    }

    fun shutdown() {
        shutdownRequested = true
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
