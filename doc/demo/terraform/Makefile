init:
	terraform init
gcp_plan:
		terraform plan -target=module.gcp -var-file=credentials.tfvars
aws_plan:
		terraform plan -target=module.aws
azure_plan:
		terraform plan -target=module.azure -var-file=credentials.tfvars

gcp_apply:
	terraform apply -target=module.gcp -var-file=credentials.tfvars
aws_apply:
	terraform apply -target=module.aws
azure_apply:
	terraform apply -target=module.azure -var-file=credentials.tfvars

gcp_destroy:
			terraform destroy -target=module.gcp -var-file=credentials.tfvars
aws_destroy:
	terraform destroy -target=module.aws
azure_destroy:
	terraform destroy -target=module.azure -var-file=credentials.tfvars
