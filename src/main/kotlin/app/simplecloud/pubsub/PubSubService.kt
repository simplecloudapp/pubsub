package app.simplecloud.pubsub

import build.buf.gen.simplecloud.pubsub.v1.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class PubSubService : PubSubServiceGrpcKt.PubSubServiceCoroutineImplBase() {

    private val subscribers = ConcurrentHashMap<String, MutableList<ProducerScope<Message>>>()
    private val mutex = Mutex()

    override fun subscribe(request: SubscriptionRequest): Flow<Message> = callbackFlow {
        val topic = request.topic

        mutex.withLock {
            subscribers.getOrPut(topic) { mutableListOf() }.add(this)
        }

        awaitClose {
            runBlocking {
                unsubscribe(topic, this@callbackFlow)
            }
        }
    }

    private suspend fun unsubscribe(topic: String, scope: ProducerScope<Message>) {
        mutex.withLock {
            subscribers[topic]?.remove(scope)
            if (subscribers[topic].isNullOrEmpty()) {
                subscribers.remove(topic)
            }
        }
    }

    override suspend fun publish(request: PublishRequest): PublishResponse {
        val topic = request.topic
        val messageBody = request.messageBody

        val message = message {
            this.topic = topic
            this.messageBody = messageBody
            this.timestamp = System.currentTimeMillis()
        }

        mutex.withLock {
            subscribers[topic]?.forEach { collector ->
                collector.trySend(message)
            }
        }

        return publishResponse {
            this.success = true
        }
    }
}
