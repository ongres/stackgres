# Create the AWS, GCP and Azure infrastructure with Terraform

Create a platform for Cloud with Kubernetes

## Requirements

[Install Terreform ](https://www.terraform.io/downloads.html)


## Setup

Depending on the cloud in which you want to deploy your infrastructure, choose the settings you need:

1.- Export variables of AWS
```bash
export AWS_ACCESS_KEY_ID=<your information>
export AWS_SECRET_ACCESS_KEY=<your information>
export AWS_DEFAULT_REGION=<your information>
```
2.- Export variables of   GCP

3.- Create file credentials.tfvars  AZURE

* Add `client_id` and `client_secret` this file


First, initiate Terraform

```
make init
```
Then execute `make` depending you cloud, the options are:  *gcp, azure, aws*
For example
```
make gcp_plan
make gcp_apply
```
