package lambda

import (
	"context"
	"errors"
	"strings"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/aws/arn"
	"github.com/aws/aws-sdk-go-v2/service/lambda"
	lambdatypes "github.com/aws/aws-sdk-go-v2/service/lambda/types"
	tagapi "github.com/aws/aws-sdk-go-v2/service/resourcegroupstaggingapi"
	tagtypes "github.com/aws/aws-sdk-go-v2/service/resourcegroupstaggingapi/types"

	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource"
)

type Finder struct {
	c *lambda.Client
	r *tagapi.Client
}

func NewFinder(cp aws.CredentialsProvider, region string) *Finder {
	c := lambda.New(lambda.Options{Credentials: cp, Region: region})
	r := tagapi.New(tagapi.Options{Credentials: cp, Region: region})
	return &Finder{c, r}
}

func (t *Finder) FindResources(ctx context.Context, cfg resource.Config) ([]resource.Resource, error) {
	if len(cfg.Include) > 0 {
		return t.findByName(ctx, cfg.Include)
	}

	return t.findByTag(ctx, cfg.Tags, cfg.Exclude)
}

func (t *Finder) findByName(ctx context.Context, names []string) ([]resource.Resource, error) {
	var r []resource.Resource
	for _, v := range names {
		f, err := t.c.GetFunction(ctx, &lambda.GetFunctionInput{
			FunctionName: aws.String(v),
		})

		if err != nil {
			var errorType *lambdatypes.ResourceNotFoundException
			if errors.As(err, &errorType) {
				continue
			}

			return nil, err
		}

		version := "N/A"
		if tagValue, ok := f.Tags["version"]; ok && tagValue != "" {
			version = tagValue
		}

		r = append(r, resource.Resource{
			Type:    "Lambda",
			Name:    v,
			Version: version,
		})
	}

	return r, nil
}

func (t *Finder) findByTag(ctx context.Context, tags map[string][]string, exclude []string) ([]resource.Resource, error) {
	excluded, err := resource.ExcludedAsRegexp(exclude)
	if err != nil {
		return nil, err
	}

	input := tagapi.GetResourcesInput{
		TagFilters:          t.toTagFilters(tags),
		ResourceTypeFilters: []string{"lambda"},
	}

	response, err := t.r.GetResources(ctx, &input)
	if err != nil {
		return nil, err
	}

	out := response.ResourceTagMappingList
	for *response.PaginationToken != "" {
		input.PaginationToken = response.PaginationToken
		response, err = t.r.GetResources(ctx, &input)
		if err != nil {
			return nil, err
		}

		out = append(out, response.ResourceTagMappingList...)
	}

	var r []resource.Resource
	for _, v := range out {
		name := t.extractName(v)
		if resource.IsExcluded(name, excluded) {
			continue
		}

		r = append(r, resource.Resource{
			Type:    "Lambda",
			Name:    name,
			Version: extractVersion(v.Tags),
		})
	}

	return r, nil
}

func (t *Finder) toTagFilters(tags map[string][]string) []tagtypes.TagFilter {
	var out []tagtypes.TagFilter
	for k, v := range tags {
		f := tagtypes.TagFilter{
			Key:    aws.String(k),
			Values: v,
		}

		out = append(out, f)
	}

	return out
}

func (t *Finder) extractName(rtm tagtypes.ResourceTagMapping) string {
	parsed, _ := arn.Parse(*rtm.ResourceARN)
	return strings.Split(parsed.Resource, ":")[1]
}

func extractVersion(tags []tagtypes.Tag) string {
	version := "N/A"
	for _, tag := range tags {
		if *tag.Key != "version" {
			continue
		}

		version = *tag.Value
	}

	return version
}
