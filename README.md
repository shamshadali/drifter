# Drifter

**What**

`drifter` is a utility which helps to track "version drift" of components across different environments.

**Languages Available**

- **Go Version**: Original implementation in `main.go` and `internal/` directory
- **Java Version**: Migrated implementation in `src/main/java/` directory with Maven build

## Quick Start

### For Java Version (Recommended)
See [README-java.md](README-java.md) for complete Java documentation including:
- Prerequisites (Java 17+, Maven 3.6+)
- Build instructions
- Usage examples
- Configuration guide

### For Go Version (Legacy)
Continue reading this document for Go-specific instructions.

**Why**

In current paradigm, the latest stable version of a component must be immediately promoted to the production, but in a larger scale projects (eg 13 Lambdas and 9 services) it becomes very hard to compare versions across environments and decide what is lagging behind and must be deployed.

`drifter` aims to provide a quick overview of components whose versions are not identical between configured environments.

**How**

With Lambda deployments, `drifter` tries to check `version` tag of the Lambda function. If it's not present, version will be listed as `N/A`

With k8s services, `drifter` extracts image version from the Deployment spec.

**Installation**


If `$GOPATH` is not defined, module is going to be installed into `$HOME/go/bin`

For system-wide installation add `$GOPATH/bin` (if you have it defined) or `$HOME/go/bin` to system `$PATH`.

**Configuration**

Example configuration in JSON format

```json
{
  // list of environments to collect versions from
  "envs": [
    {
      "name": "dev",
      "region": "us-east-1",
      "role": "arn:aws:iam::<AWS account info>",
      "clusterName": "<cluster name>"
    },
    {
      "name": "stg",
      "region": "us-east-1",
      "role": "arn:aws:iam::<AWS account info>",
      "clusterName": "<cluster info>"
    },
    {
      "name": "prod",
      "region": "us-east-1",
      "role": "arn:aws:iam::<AWS account info>",
      "clusterName": "<cluster name>"
    }
  ],
  // list of lambdas you wish to track
  "lambdas": [
    "your-test-lambda-1",
    "your-test-lambda-2"
  ],
  // list of k8s deployments you wish to track
  "deployments": [
    {
      "name": "k8s-service-name",
      "namespace": "app"
    }
  ]
}
```

Example configuration in YAML format
```yaml
envs:
- name: dev
  region: us-east-1
  role: arn:aws:iam::<AWS account info>"
  cluster_name: "<cluster name>
- name: stg
  region: us-east-1
  role: arn:aws:iam::<AWS account info>"
  cluster_name: "<cluster name>
- name: prod
  region: us-east-1
  role: arn:aws:iam::<AWS account info>"
  cluster_name: "<cluster name>
lambdas:
- your-lambda-name
deployments:
- name: k8s0-service-name
  namespace: app

```

**Usage**

NOTE: your AWS credentials chain must be configured and capable of assuming roles defined in the `envs` section of the configuration file

```shell
drifter
  -config config path json or yaml format, defaults to ./config.json
  -format output format, "table" or "json"
  -verbose enable verbose output
```

Example table output:

```
#       TYPE            NAME                            DEV                             STG
1       Lambda          your-lambda-name                1.6.1-c910b5a0                  ❌ 1.6.0-6cd757e0        
2       Deployment      k8s-service-name                1.22.3                          ✅ ️1.22.3                

```

Example JSON output:

```
[
  {
    "name": "your-lambda-name",
    "type": "Lambda",
    "versions": {
      "dev": "1.6.1-c910b5a0",
      "prod": "1.6.0-6cd757e0",
      "stg": "1.6.0-6cd757e0"
    }
  }
]

```

How to use

```
# Build the Java version
./build.sh

# Run with default config
java -jar target/drifter-1.0.0.jar

# Run with custom config and JSON output  
java -jar target/drifter-1.0.0.jar -c config.yaml -f json -v

```
