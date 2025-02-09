package config

import (
	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource"
)

type Config struct {
	Envs        []Env           `json:"envs" yaml:"envs"`
	Lambdas     resource.Config `json:"lambdas" yaml:"lambdas"`
	Deployments resource.Config `json:"deployments" yaml:"deployments"`
}

type Env struct {
	Name        string `json:"name" yaml:"name"`
	Region      string `json:"region" yaml:"region"`
	Role        string `json:"role" yaml:"role"`
	ClusterName string `json:"clusterName" yaml:"clusterName"`
}
