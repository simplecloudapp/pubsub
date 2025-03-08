package main

import (
	"log"
	"sync"
	"time"

	"buf.build/gen/go/simplecloud/proto-specs/grpc/go/simplecloud/pubsub/v1/pubsubv1grpc"
	pubsubv1 "buf.build/gen/go/simplecloud/proto-specs/protocolbuffers/go/simplecloud/pubsub/v1"
)

type PubSubServer struct {
	pubsubv1grpc.UnimplementedPubSubServiceServer
	mu          sync.Mutex
	subscribers map[string][]chan *pubsubv1.Message
}

func NewPubSubServer() *PubSubServer {
	return &PubSubServer{
		subscribers: make(map[string][]chan *pubsubv1.Message),
	}
}

func (s *PubSubServer) Subscribe(req *pubsubv1.SubscriptionRequest, stream pubsubv1grpc.PubSubService_SubscribeServer) error {
	topic := req.Topic
	msgCh := make(chan *pubsubv1.Message, 10)

	s.mu.Lock()
	s.subscribers[topic] = append(s.subscribers[topic], msgCh)
	s.mu.Unlock()

	defer func() {
		s.mu.Lock()
		subs := s.subscribers[topic]
		for i, ch := range subs {
			if ch == msgCh {
				s.subscribers[topic] = append(subs[:i], subs[i+1:]...)
				break
			}
		}
		if len(s.subscribers[topic]) == 0 {
			delete(s.subscribers, topic)
		}
		s.mu.Unlock()
	}()

	for {
		select {
		case msg, ok := <-msgCh:
			if !ok {
				return nil
			}
			if err := stream.Send(msg); err != nil {
				return err
			}
		case <-stream.Context().Done():
			return nil
		}
	}
}

func (s *PubSubServer) Publish(req *pubsubv1.PublishRequest) (*pubsubv1.PublishResponse, error) {
	topic := req.Topic
	messageBody := req.MessageBody

	msg := &pubsubv1.Message{
		Topic:       topic,
		MessageBody: messageBody,
		Timestamp:   time.Now().UnixMilli(),
	}

	s.mu.Lock()
	subs := s.subscribers[topic]
	s.mu.Unlock()

	for _, ch := range subs {
		select {
		case ch <- msg:
		default:
			log.Printf("Warning: subscriber channel for topic %s is full", topic)
		}
	}

	return &pubsubv1.PublishResponse{Success: true}, nil
}
