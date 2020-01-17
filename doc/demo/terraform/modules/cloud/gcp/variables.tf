

// Information about VPC

variable "network_name" {}
variable "project_vpc" {}
//  Configuration subnet
variable "subnet_name" {}
variable "subnet_ip" {}
variable "subnet_region" {}
//secondary_ranges
variable "range_name_pods" {}
variable "ip_cidr_range_pods" {}
variable "range_name_services" {}
variable "ip_cidr_range_services" {}

// Information about GKE
variable "project_gke" {}
variable "name_gke" {}
variable "region_gke" {}
variable "zones_gke" {}

//  Information about node pools (np)

variable "name_np" {}
variable "machine_type_np" {}
variable "min_count_np" {}
variable "max_count_np" {}
variable "disk_size_gb_np" {}
variable "service_account_np" {}
variable "initial_node_count_np" {}
variable "value_np" {}
