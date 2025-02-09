**What**

`drifter` is a utility which helps to track "version drift" of components across different environments. 

**Why**

In current paradigm, the latest stable version of a component must be immediately promoted to the production, but in a larger scale projects such as OTC (13 Lambdas and 9 services) it becomes very hard to compare versions across environments and decide what is lagging behind and must be deployed.

`drifter` aims to provide a quick overview of components whose versions are not identical between configured environments.

**How**

With Lambda deployments, `drifter` tries to check `version` tag of the Lambda function. If it's not present, version will be listed as `N/A`

With k8s services, `drifter` extracts image version from the Deployment spec.

**Installation**

`go install gopkg.mgmt.carezen.net/~konstantyn.kuiun/drifter@latest`

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
      "role": "arn:aws:iam::314840214426:role/dev-bravo-omni-dev",
      "clusterName": "omni-dev-useast1-kubemaster"
    },
    {
      "name": "stg",
      "region": "us-east-1",
      "role": "arn:aws:iam::211958814005:role/dev-bravo-omni-stg",
      "clusterName": "omni-stg-useast1-kubemaster"
    },
    {
      "name": "prod",
      "region": "us-east-1",
      "role": "arn:aws:iam::950940341780:role/dev-bravo-omni-prod",
      "clusterName": "omni-prod-useast1-kubemaster"
    }
  ],
  // list of lambdas you wish to track
  "lambdas": [
    "pn-otb-seeker-notifier",
    "pn-otb-provider-notifier",
    "pn-otb-ttl-remover",
    "pn-otb-status-handler",
    "pn-otb-job-event-emitter",
    "pn-otb-job-posting-event-emitter",
    "pn-otb-expiration-watcher",
    "pn-otb-next-tier-watcher",
    "pn-otb-next-batch-watcher",
    "pn-otb-data-lake-forwarder",
    "pn-caregiver-cdc-publisher",
    "pn-otb-invite-count-updater"
  ],
  // list of k8s deployments you wish to track
  "deployments": [
    {
      "name": "pn-job-posting",
      "namespace": "pn"
    },
    {
      "name": "pn-job-fulfillment",
      "namespace": "pn"
    },
    {
      "name": "pn-provider-search",
      "namespace": "pn"
    }
  ]
}
```

Example configuration in YAML format
```yaml
envs:
- name: dev
  region: us-east-1
  role: arn:aws:iam::314840214426:role/dev-bravo-omni-dev
  cluster_name: omni-dev-useast1-kubemaster
- name: stg
  region: us-east-1
  role: arn:aws:iam::211958814005:role/dev-bravo-omni-stg
  cluster_name: omni-stg-useast1-kubemaster
- name: prod
  region: us-east-1
  role: arn:aws:iam::950940341780:role/dev-bravo-omni-prod
  cluster_name: omni-prod-useast1-kubemaster
lambdas:
- pn-caregiver-cdc-publisher
- pn-otb-data-lake-forwarder
- pn-otb-expiration-watcher
- pn-otb-invite-count-updater
- pn-otb-job-event-emitter
- pn-otb-job-posting-event-emitter
- pn-otb-next-batch-watcher
- pn-otb-next-tier-watcher
- pn-otb-provider-notifier
- pn-otb-seeker-notifier
- pn-otb-status-handler
- pn-otb-ttl-remover
deployments:
- name: pn-job-fulfillment
  namespace: pn
- name: pn-job-posting
  namespace: pn
- name: pn-provider-search
  namespace: pn

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
1       Lambda          pn-caregiver-cdc-publisher      1.6.1-c910b5a0                  ❌ 1.6.0-6cd757e0        
2       Lambda          pn-otb-expiration-watcher       1.2.5-51b3aeb5                  ✅ ️️1.2.5-51b3aeb5       
3       Lambda          pn-otb-next-batch-watcher       1.3.1-679e1ccc                  ✅ ️1.3.1-679e1ccc        
4       Lambda          pn-otb-status-handler           1.4.5-416b1aa3                  ✅ 1.4.5-416b1aa3        
5       Deployment      pn-job-fulfillment              1.36.5-SNAPSHOT-PN-2046         ❌ 1.36.4               
6       Deployment      pn-job-posting                  1.22.3                          ✅ ️1.22.3                

```

Example JSON output:

```
[
  {
    "name": "pn-caregiver-cdc-publisher",
    "type": "Lambda",
    "versions": {
      "dev": "1.6.1-c910b5a0",
      "prod": "1.6.0-6cd757e0",
      "stg": "1.6.0-6cd757e0"
    }
  },
  {
    "name": "pn-job-posting",
    "type": "Deployment",
    "versions": {
      "dev": "1.22.3",
      "prod": "1.22.0",
      "stg": "1.22.3"
    }
  }
]

```
