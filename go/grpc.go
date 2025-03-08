package pubsub

import (
	"context"
	"log"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

type AuthCallCredentials struct {
	SecretKey string
}

func (a *AuthCallCredentials) GetRequestMetadata(context.Context, ...string) (map[string]string, error) {
	return map[string]string{
		"Authorization": a.SecretKey,
	}, nil
}

func (a *AuthCallCredentials) RequireTransportSecurity() bool {
	return false
}

func CreateConnection(hostAddress string, secretKey string) *grpc.ClientConn {
	var opts []grpc.DialOption
	opts = append(opts, grpc.WithTransportCredentials(insecure.NewCredentials()))
	opts = append(opts, grpc.WithPerRPCCredentials(&AuthCallCredentials{
		SecretKey: secretKey,
	}))
	conn, err := grpc.NewClient(hostAddress, opts...)
	if err != nil {
		log.Fatalf("Failed to connect to simplecloud: %v", err)
	}

	return conn
}
