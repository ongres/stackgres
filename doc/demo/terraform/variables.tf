variable "region_gcp" {
  default = "us-central1"
  description = "The Name of the region"
}

variable "project" {
    default = "stackgres-demo-256115"
    description = "The name of projects in GCP"
}

variable "profile" {
    default = "benchmark"
    description = "Name profile"
}


variable "region_aws" {
  default = "us-west-2"
  description = "Name region"
}


variable "client_id" {}
variable "client_secret" {}
