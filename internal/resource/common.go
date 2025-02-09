package resource

import (
	"regexp"
)

type Config struct {
	Namespace string
	Tags      map[string][]string
	Exclude   []string
	Include   []string
}

type Resource struct {
	Type    string
	Name    string
	Version string
}

type Finder interface {
	FindResources(cfg Config) []Resource
}

func IsExcluded(name string, expr []*regexp.Regexp) bool {
	for _, v := range expr {
		if v.MatchString(name) {
			return true
		}
	}

	return false
}

func ExcludedAsRegexp(exclude []string) ([]*regexp.Regexp, error) {
	excluded := make([]*regexp.Regexp, len(exclude))
	for i, v := range exclude {
		e, err := regexp.Compile(v)
		if err != nil {
			return nil, err
		}

		excluded[i] = e
	}

	return excluded, nil
}
