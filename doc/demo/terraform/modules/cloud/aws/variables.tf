// Information about VPC

variable "name_vpc" {}
variable "cidr_vpc" {}
variable "azs_vpc" {}
variable "private_subnets_vpc" {}
variable "public_subnets_vpc"{}
variable "enable_nat_gateway_vpc" {}
variable "enable_vpn_gateway_vpc" {}
variable "enable_dns_hostnames_vpc" {}


// Information about cluster

variable "cluster_name_eks" {}
variable "instance_type_eks"{}
variable "asg_max_size_eks" {}
variable "cluster_version_eks" {}
