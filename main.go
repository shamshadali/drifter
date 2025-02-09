package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"strings"
	"sync"
	"text/tabwriter"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials/stscreds"
	"github.com/aws/aws-sdk-go-v2/service/eks"
	"github.com/aws/aws-sdk-go-v2/service/sts"

	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource"
	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource/k8s"
	"gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/resource/lambda"

	cfg "gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter/internal/config"
)

func main() {
	cfgPath := flag.String("config", "config.json", "config file location, json or yaml format")
	verbose := flag.Bool("verbose", false, "enable verbose output")
	format := flag.String("format", "table", "output format, json or table")
	flag.Parse()

	defer func(n time.Time) {
		log.Printf("done in %dms", time.Since(n).Milliseconds())
	}(time.Now())

	c, err := cfg.ReadFile(*cfgPath)
	if err != nil {
		log.Fatal(err)
	}

	if !*verbose {
		log.SetOutput(io.Discard)
	}

	log.Print("loading AWS config")

	ctx := context.Background()
	awscfg, err := awsconfig.LoadDefaultConfig(ctx, awsconfig.WithDefaultRegion("us-east-1"))
	if err != nil {
		log.Fatal(err)
	}

	log.Print("creating STS client")
	stsClient := sts.NewFromConfig(awscfg)

	m := &sync.Mutex{}
	wg := &sync.WaitGroup{}
	resources := make(map[string][]resource.Resource)
	for _, env := range c.Envs {
		p := assumeRole(ctx, stsClient, env.Role)

		wg.Add(1)
		log.Printf("fetching %s functions", env.Name)
		go func(ctx context.Context, env string, c resource.Config, p aws.CredentialsProvider) {
			lambdas := fetchLambdas(ctx, c, p)
			m.Lock()
			resources[env] = append(resources[env], lambdas...)
			m.Unlock()
			wg.Done()
		}(ctx, env.Name, c.Lambdas, p)

		awscfg.Credentials = p
		eksClient := eks.NewFromConfig(awscfg)

		wg.Add(1)
		log.Printf("fetching %s services", env.Name)
		go func(ctx context.Context, env cfg.Env, c resource.Config, client *eks.Client) {
			deployments := fetchDeployments(ctx, c, env, client)
			m.Lock()
			resources[env.Name] = append(resources[env.Name], deployments...)
			m.Unlock()
			wg.Done()
		}(ctx, env, c.Deployments, eksClient)
	}

	wg.Wait()
	if *format == "table" {
		writeTable(c.Envs, resources)
		return
	}

	if *format == "json" {
		json.NewEncoder(os.Stdout).Encode(resources)
	}
}

func assumeRole(ctx context.Context, sts *sts.Client, roleARN string) aws.CredentialsProvider {
	log.Printf("assuming role %s", roleARN)
	provider := stscreds.NewAssumeRoleProvider(sts, roleARN, func(options *stscreds.AssumeRoleOptions) {})
	return aws.NewCredentialsCache(provider)
}

func fetchLambdas(ctx context.Context, c resource.Config, p aws.CredentialsProvider) []resource.Resource {
	f := lambda.NewFinder(p, "us-east-1")
	lambdas, err := f.FindResources(ctx, c)
	if err != nil {
		log.Fatal(err)
	}

	return lambdas
}

func fetchDeployments(ctx context.Context, c resource.Config, env cfg.Env, eksClient *eks.Client) []resource.Resource {
	log.Printf("creating %s eks client", env.ClusterName)
	client := k8s.NewClient(ctx, eksClient, env.ClusterName, env.Role)
	f := k8s.NewDeploymentFinder(client)
	deployments, err := f.FindResources(ctx, c)
	if err != nil {
		log.Fatal(err)
	}

	return deployments
}

func writeTable(envs []cfg.Env, resources map[string][]resource.Resource) {
	w := tabwriter.NewWriter(os.Stdout, 0, 8, 4, '\t', 0)
	w = w.Init(os.Stdout, 0, 8, 4, '\t', 0)

	fmt.Fprintf(w, "#\tTYPE\tNAME\t%s\n", joinEnvs(envs, '\t'))
	count := 0

	envVersions := make(map[string][]string, 0)
	for _, env := range envs {
		r := resources[env.Name]
		for _, v := range r {
			envVersions[v.Name] = append(envVersions[v.Name], v.Version)
		}
	}

	for _, res := range resources[envs[0].Name] {
		versions, drift := detectDrift(envVersions[res.Name])
		if drift {
			count++
			fmt.Fprintf(w, "%d\t%s\t%s\t%s\t\n", count, res.Type, res.Name, strings.Join(versions, "\t"))
		}
	}

	w.Flush()
}

func joinEnvs(e []cfg.Env, sep byte) string {
	b := strings.Builder{}
	l := len(e)
	for _, v := range e[:l-1] {
		b.WriteString(v.Name)
		b.WriteByte(sep)
	}
	b.WriteString(e[l-1].Name)
	return strings.ToUpper(b.String())
}

func detectDrift(versions []string) ([]string, bool) {
	drift := false
	if versions[0] == "" {
		drift = true
		versions[0] = "NOT FOUND"
	}

	for i := 1; i < len(versions); i++ {
		ver := versions[i]
		if ver == "" {
			versions[i] = "❗ NOT FOUND"
			continue
		}

		if versions[0] == ver {
			versions[i] = fmt.Sprintf("✅ %s", ver)
			continue
		}

		versions[i] = fmt.Sprintf("❌ %s", ver)
		drift = true
	}

	return versions, drift
}
