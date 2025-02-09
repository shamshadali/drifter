package k8s

import (
	"context"
	"log"
	"regexp"

	"k8s.io/apimachinery/pkg/api/errors"
	metaV1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
)

var (
	imageRegex = regexp.MustCompile(".+:(\\d+\\.\\d+\\.\\d+(-SNAPSHOT-.+)?)")
)

type Deployment struct {
	Name      string `json:"name"`
	Namespace string `json:"namespace"`
}

func ListDeployments(ctx context.Context, k *kubernetes.Clientset, names []Deployment) map[string]string {
	m := make(map[string]string)
	for _, v := range names {
		d, err := k.AppsV1().Deployments(v.Namespace).Get(ctx, v.Name, metaV1.GetOptions{})
		if err != nil {
			stErr, ok := err.(*errors.StatusError)
			if !ok {
				log.Fatal(err)
			}

			if stErr.Status().Reason != metaV1.StatusReasonNotFound {
				log.Fatal(err)
			}

			continue
		}

		for _, container := range d.Spec.Template.Spec.Containers {
			if container.Name == v.Name {
				m[v.Name] = extractImageVersion(container.Image)
			}
		}
	}

	return m
}

func extractImageVersion(image string) string {
	submatch := imageRegex.FindStringSubmatch(image)
	if len(submatch) < 2 {
		return "N/A"
	}

	return submatch[1]
}
