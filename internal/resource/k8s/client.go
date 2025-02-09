package k8s

import (
	"context"
	"log"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/eks"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"sigs.k8s.io/aws-iam-authenticator/pkg/token"
)

func NewClient(ctx context.Context, client *eks.Client, clusterName, role string) *kubernetes.Clientset {
	result, err := client.DescribeCluster(ctx, &eks.DescribeClusterInput{Name: aws.String(clusterName)})
	if err != nil {
		log.Fatal(err)
	}

	gen, err := token.NewGenerator(false, false)
	if err != nil {
		log.Fatal(err)
	}

	tok, err := gen.GetWithRole(clusterName, role)
	if err != nil {
		log.Fatal(err)
	}

	k := kubernetes.NewForConfigOrDie(&rest.Config{
		Host: *result.Cluster.Endpoint,
		TLSClientConfig: rest.TLSClientConfig{
			Insecure: true,
		},
		BearerToken: tok.Token,
	})

	return k
}
