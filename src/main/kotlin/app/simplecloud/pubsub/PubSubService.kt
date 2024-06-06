package app.simplecloud.pubsub

import build.buf.gen.simplecloud.pubsub.v1.*
import io.grpc.stub.ServerCallStreamObserver
import io.grpc.stub.StreamObserver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class PubSubService  : PubSubServiceGrpc.PubSubServiceImplBase() {

    private val subscribers = ConcurrentHashMap<String, ConcurrentLinkedQueue<StreamObserver<Message>>>()

    override fun subscribe(request: SubscriptionRequest, responseObserver: StreamObserver<Message>) {
        val topic = request.topic
        subscribers.computeIfAbsent(topic) { k -> ConcurrentLinkedQueue() }.add(responseObserver)
        val streamResponseObserver = responseObserver as? ServerCallStreamObserver<Message> ?: return
        streamResponseObserver.setOnCloseHandler {
            subscribers.computeIfAbsent(topic) { k -> ConcurrentLinkedQueue() }.remove(responseObserver)
        }
        streamResponseObserver.setOnCancelHandler {
            subscribers.computeIfAbsent(topic) { k -> ConcurrentLinkedQueue() }.remove(responseObserver)
        }
        println("Subscribed $topic")
    }

    override fun publish(request: PublishRequest, responseObserver: StreamObserver<PublishResponse>) {
        val topic = request.topic
        val messageBody = request.messageBody

        val message = Message.newBuilder()
            .setTopic(topic)
            .setMessageBody(messageBody)
            .setTimestamp(System.currentTimeMillis())
            .build()

        var size =0

        if (subscribers.containsKey(topic)) {
            for (observer in subscribers[topic]!!) {
                observer.onNext(message)
                size++
            }
        }

        println("Published $topic ${message} ${size}")

        val response: PublishResponse = PublishResponse.newBuilder()
            .setSuccess(true)
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}