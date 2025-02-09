package k8s

import (
	"context"
	"log"

	"k8s.io/apimachinery/pkg/api/errors"
	metaV1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"

	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource"
)

type DeploymentFinder struct {
	c *kubernetes.Clientset
}

func NewDeploymentFinder(c *kubernetes.Clientset) *DeploymentFinder {
	return &DeploymentFinder{c}
}

func (d *DeploymentFinder) FindResources(ctx context.Context, cfg resource.Config) ([]resource.Resource, error) {
	if len(cfg.Include) > 0 {
		return d.findByName(ctx, cfg.Include)
	}

	return d.findByLabel(ctx, cfg.Tags, cfg.Exclude)
}

func (d *DeploymentFinder) findByName(ctx context.Context, names []string) ([]resource.Resource, error) {
	out := make([]resource.Resource, len(names))
	for _, v := range names {
		d, err := d.c.AppsV1().Deployments("").Get(ctx, v, metaV1.GetOptions{})
		if err != nil {
			stErr, ok := err.(*errors.StatusError)
			if !ok {
				log.Fatal(err)
			}

			if stErr.Status().Reason != metaV1.StatusReasonNotFound {
				return nil, err
			}

			continue
		}

		version := "N/A"
		for _, container := range d.Spec.Template.Spec.Containers {
			if container.Name == v {
				version = extractImageVersion(container.Image)
			}
		}

		out = append(out, resource.Resource{
			Type:    "Deployment",
			Name:    v,
			Version: version,
		})
	}

	return out, nil
}

func (d *DeploymentFinder) findByLabel(ctx context.Context, labels map[string][]string, exclude []string) ([]resource.Resource, error) {
	excluded, err := resource.ExcludedAsRegexp(exclude)
	if err != nil {
		return nil, err
	}

	opts := metaV1.ListOptions{
		TypeMeta: metaV1.TypeMeta{
			Kind: "Deployment",
		},
		LabelSelector: d.labelSelector(labels),
	}

	list, err := d.c.AppsV1().Deployments("").List(ctx, opts)
	if err != nil {
		return nil, err
	}

	var out []resource.Resource
	for _, v := range list.Items {
		r := resource.Resource{
			Type: "Deployment",
			Name: v.Name,
		}

		for _, container := range v.Spec.Template.Spec.Containers {
			if resource.IsExcluded(container.Name, excluded) {
				continue
			}

			r.Version = extractImageVersion(container.Image)
			out = append(out, r)
		}
	}

	return out, nil
}

func (d *DeploymentFinder) labelSelector(labels map[string][]string) string {
	ls := metaV1.LabelSelector{
		MatchLabels: map[string]string{},
	}
	for key, values := range labels {
		if len(values) == 1 {
			ls.MatchLabels[key] = values[0]
			continue
		}

		lsr := metaV1.LabelSelectorRequirement{
			Key:      key,
			Operator: metaV1.LabelSelectorOpIn,
			Values:   values,
		}
		ls.MatchExpressions = append(ls.MatchExpressions, lsr)
	}

	return metaV1.FormatLabelSelector(&ls)
}
