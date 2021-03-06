# Create the AWS infrastructure with Terraform

## Begin 🚀

_These instructions will allow you to create Cluster Kubernetes in AWS_

### Prerequisite 📋

* Installing aws-iam-authenticator

[aws-iam-authenticator](https://docs.aws.amazon.com/eks/latest/userguide/install-aws-iam-authenticator.html)

### Installation 🔧
* Install `eks`

  * [Getting Started with eksctl](https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html)

* Install `kubectl`

  * [Install and Set Up kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)

* Install `terraform`

  * [Terraform](https://learn.hashicorp.com/terraform/getting-started/install.html)

## Platform Deployment with Terraform  ⚙️

1.- Initialize a working directory containing Terraform configuration files:  

`terraform init`

2.- Create an execution plan:

`terraform plan -target=module.aws`

3.- Deployment the infrastructure:  

`terraform apply -target=module.aws`

[For more information](https://www.terraform.io/docs/commands/index.html)

> In the case of wanting to destroy the platform you can execute `terraform destroy`

### Verify connection the Cluster
##### eksctl container clusters get-credentials
`aws eks --region ${AWS_REGION} update-kubeconfig --name ${AWS_CLUSTER_NAME}`

> [For more information]()

#### Information about cluster

`kubectl cluster-info`

Now, enjoy

[Install Stackgres](https://gitlab.com/ongresinc/stackgres/blob/124-write-stackgres-0-8-documentation/doc/demo/gcloud.md)


---
## Inputs about Module GCP


This module create a VPC and Cluster Kubernetes

Name|Description|	Type|	Default|	Required
---|---|---|---|---|
name_vpc | Name to be used on all the resources as identifier| string|""|no
cidr_vpc | The CIDR block for the VPC. Default value is a valid CIDR, but not acceptable by AWS and should be overridden| tring|"0.0.0.0/0"|no
azs_vpc | A list of availability zones in the region| list(string)|	[]|	no
private_subnets_vpc | A list of private subnets inside the VPC|	list(string)|	[]|	no
public_subnets_vpc | A list of public subnets inside the VPC|	list(string)|	[]|	no
enable_nat_gateway_vpc| Should be true if you want to provision NAT Gateways for each of your private networks|	bool|	"false"|	no
enable_vpn_gateway_vpc| Should be true if you want to create a new VPN Gateway resource and attach it to the VPC|	bool|	"false"|	no
enable_dns_hostnames_vpc| Should be true to enable DNS hostnames in the Default VPC|	bool|	"false"|	no
cluster_name_eks|Name of the EKS cluster. Also used as a prefix in names of related resources|	string|	n/a|	yes
cluster_version_eks | Kubernetes version to use for the EKS cluster|	string|	"1.14"|	no
instance_type_eks | The instance type that you specify determines the hardware of the host computer used for your instance| string|n/a|yes
asg_max_size_eks| The maximum values for scaling | string | n/a| yes
