# Create the Azure infrastructure with Terraform

## Begin 🚀

_These instructions will allow you to create Cluster Kubernetes in Azure_

### Prerequisite 📋

* Azure Provider

[Authenticating using a Service Principal with a Client Secret](https://www.terraform.io/docs/providers/azurerm/guides/service_principal_client_secret.html)

### Installation 🔧
* Install `az`

  * [Install the Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest)

* Install `kubectl`

  * [Install and Set Up kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

* Install `terraform`

  * [Terraform](https://learn.hashicorp.com/terraform/getting-started/install.html)

## Platform Deployment with Terraform  ⚙️

1.- Create `file credentials.tfvars`

2.- Add `client_id` and `client_secret` this file

3.- Initialize a working directory containing Terraform configuration files:  

`terraform init`

4.- Create an execution plan:

`terraform plan -target=module.azure  -var-file=credentials.tfvars`

5.- Deployment the infrastructure:  

`terraform apply -target=module.azure  -var-file=credentials.tfvars`

[For more information](https://www.terraform.io/docs/commands/index.html)

> In the case of wanting to destroy the platform you can execute `terraform destroy -var-file=credentials.tfvars`

### Verify connection the Cluster
##### az container clusters get-credentials
`az aks get-credentials --resource-group ${AZURE_GROUP} --name ${AZURE_CLUSTER_NAME} `
> [For more information]()

#### Information about cluster

`kubectl cluster-info`

Now, enjoy

[Install Stackgres](https://gitlab.com/ongresinc/stackgres/blob/124-write-stackgres-0-8-documentation/doc/demo/gcloud.md)


---
## Inputs about Module GCP


This module create Cluster Kubernetes

Name|Description|	Type|	Default|	Required
---|---|---|---|---|
g_name_az |The name of the resource group. Must be unique on your Azure subscription| string|n/a|yes
g_location_az|The location where the resource group should be created. For a list of all Azure locations, please consult this link or run az account list-locations --output table| string|n/a|yes
name_az|The name of the Managed Kubernetes Cluster to create. Changing this forces a new resource to be created|string|n/a|yes
dns_prefix_az|(Required) DNS prefix specified when creating the managed cluster. Changing this forces a new resource to be created|string|n/a|
kubernetes_version_az|(Optional) Version of Kubernetes specified when creating the AKS managed cluster. If not specified, the latest recommended version will be used at provisioning time (but won't auto-upgrade)|string|n/a|no
name_np|The name node pools | string | n/a| yes
node_count_np|(Optional) The name of the Resource Group where the Kubernetes Nodes should exist. Changing this forces a new resource to be created|string|n/a|no
vm_size_az |(Required) The size of each VM in the Agent Pool (e.g. Standard_F1). Changing this forces a new resource to be created|string|n/a|yes
client_id |(Required) The Client ID for the Service Principal|string|n/a|yes
client_secret|(Required) The Client ID for the Service Principal|string|n/a|yes
