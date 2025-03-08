package main

import (
	"buf.build/gen/go/simplecloud/proto-specs/grpc/go/simplecloud/pubsub/v1/pubsubv1grpc"
	pubsubv1 "buf.build/gen/go/simplecloud/proto-specs/protocolbuffers/go/simplecloud/pubsub/v1"
	"context"
	"log"
	"sync"
	"time"

	"google.golang.org/grpc"
)

type PubSubListener func(msg *pubsubv1.Message)

type PubSubClient struct {
	conn           *grpc.ClientConn
	client         pubsubv1grpc.PubSubServiceClient
	topicListeners map[string][]PubSubListener
	mu             sync.Mutex
}

func NewPubSubClient(hostAddress string, secretKey string) (*PubSubClient, error) {
	conn := CreateConnection(hostAddress, secretKey)
	client := pubsubv1grpc.NewPubSubServiceClient(conn)
	return &PubSubClient{
		conn:           conn,
		client:         client,
		topicListeners: make(map[string][]PubSubListener),
	}, nil
}

func (p *PubSubClient) Subscribe(topic string, listener PubSubListener) {
	p.mu.Lock()
	p.topicListeners[topic] = append(p.topicListeners[topic], listener)
	p.mu.Unlock()

	go p.subscribeToTopic(topic)
}

func (p *PubSubClient) subscribeToTopic(topic string) {
	for {
		ctx, cancel := context.WithCancel(context.Background())
		stream, err := p.client.Subscribe(ctx, &pubsubv1.SubscriptionRequest{
			Topic: topic,
		})
		if err != nil {
			log.Printf("Error subscribing to topic %s: %v. Retrying...", topic, err)
			cancel()
			time.Sleep(2 * time.Second)
			continue
		}
		for {
			msg, err := stream.Recv()
			if err != nil {
				log.Printf("Error receiving message for topic %s: %v. Re-subscribing...", topic, err)
				cancel()
				time.Sleep(2 * time.Second)
				break
			}
			p.mu.Lock()
			listeners := p.topicListeners[topic]
			p.mu.Unlock()
			for _, l := range listeners {
				l(msg)
			}
		}
	}
}

func (p *PubSubClient) Publish(topic string, messageBody *pubsubv1.MessageBody) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	_, err := p.client.Publish(ctx, &pubsubv1.PublishRequest{
		Topic:       topic,
		MessageBody: messageBody,
	})
	return err
}

func (p *PubSubClient) Close() error {
	return p.conn.Close()
}
