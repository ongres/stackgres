# Create the GKE infrastructure with Terraform

## Begin 🚀

_These instructions will allow you to create Cluster Kubernetes in GCP_

### Prerequisite 📋
Take the following steps to enable the Kubernetes Engine API:

1. Visit the [Kubernetes Engine page](https://console.cloud.google.com/projectselector/kubernetes) in the Google Cloud Console
2. Create or select a project
3. Wait for the API and related services to be enabled. This can take several minutes
4. Make sure that billing is enabled for your Google Cloud project. [Learn how to confirm billing is enabled for your project](https://cloud.google.com/billing/docs/how-to/modify-project)

### Installation 🔧
* Install `gcloud`

  * [Quickstart for Debian and Ubuntu](https://cloud.google.com/sdk/docs/quickstart-debian-ubuntu)

  * [Quickstart for macOS](https://cloud.google.com/sdk/docs/quickstart-macos)

* Install `kubectl`

  * [Install and Set Up kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

* Install `terraform`

  * [Terraform](https://learn.hashicorp.com/terraform/getting-started/install.html)

## Platform Deployment with Terraform  ⚙️

1.- Initialize a working directory containing Terraform configuration files:  

`terraform init`

2.- Create an execution plan:

`terraform plan -target=module.gcp`

3.- Deployment the infrastructure:  

`terraform apply -target=module.gcp`

[For more information](https://www.terraform.io/docs/commands/index.html)

> In the case of wanting to destroy the platform you can execute `terraform destroy`


### Verify connection the Cluster

##### gcloud container clusters get-credentials
`gcloud container clusters get-credentials ${GOOGLE_CLUSTER_NAME}  --zone=${GOOGLE_ZONE}`

> [For more information](https://cloud.google.com/sdk/gcloud/reference/container/clusters/get-credentials?hl=es)

#### Information about cluster

`kubectl cluster-info`

Now, enjoy

[Install Stackgres](https://gitlab.com/ongresinc/stackgres/blob/124-write-stackgres-0-8-documentation/doc/demo/gcloud.md)

---
## Inputs about Module GCP


This module create a VPC and Cluster Kubernetes

Name|Description|	Type|	Default|	Required
---|---|---|---|---|
project_vpc | The project ID to host the cluster in (required) |string| n/a | yes
network_name | The VPC network to host the cluster in (required) | string | n/a | yes
subnet_name | The subnetwork to host the cluster in (required) | string | n/a| yes
subnet_ip | The IP and CIDR range of the subnet being created|string|- | yes
subnet_region| The region to host the cluster in (optional if zonal cluster / required if regional) |string | "null" |no
range_name_pods | The name of the secondary subnet ip range to use for pods |string | n/a | yes
range_services| The name of the secondary subnet range to use for services | string | n/a| yes
ip_cidr_range_pods | The range ip to use for pods | string | - | yes
ip_cidr_range_services | The range ip to use for services | string | - | yes
project_gke | The project ID to host the cluster in (required) |string| n/a | yes
name_gke | The name of the cluster (required) |string| n/a | yes
region_gke| The region to host the cluster in (optional if zonal cluster / required if regional)|	string|	"null"|	no
zones_gke | The zones to host the cluster in (optional if regional cluster / required if zonal)|	list(string)	|<list>|	no
name_np | The name node pools | string | n/a| yes
machine_type_np | The type machine | string | n/a |yes
min_count_np | The minimal  number of nodes | string | n/a|yes
max_count_np  | The maximum number of nodes | string | n/a|yes
disk_size_gb_np | The disk size for the machine |string | 100 |n/a|  yes
service_account_np | The service account to run nodes as if not overridden innode_pools. The create_service_account variable default value (true) will cause a cluster-specific service account to be created.The service account to run nodes as if not overridden innode_pools. The create_service_account variable default value (true) will cause a cluster-specific service account to be created. | string| "" | yes
initial_node_count_np |The number of nodes to create in this cluster's default node pool | number | "0"| no
value_np | The name for the node pool metadata custom | string| n/a| no


> These are all some of the configurable variables within the `VPC` and `GKE` modules, you can change it according to your requirements

[For more information about Module VPC](https://registry.terraform.io/modules/terraform-google-modules/network/google/2.0.0)

[For more information about Module GKE](https://registry.terraform.io/modules/terraform-google-modules/kubernetes-engine/google/6.1.1)
