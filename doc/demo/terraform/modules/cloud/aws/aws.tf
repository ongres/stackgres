
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "2.21.0"
  name    = var.name_vpc
  cidr    = var.cidr_vpc

  azs             = var.azs_vpc
  private_subnets = var.private_subnets_vpc
  public_subnets  = var.public_subnets_vpc

  enable_nat_gateway   = var.enable_vpn_gateway_vpc
  enable_vpn_gateway   = var.enable_vpn_gateway_vpc
  enable_dns_hostnames = var.enable_dns_hostnames_vpc

  tags = {
    Terraform   = "true"
    Environment = "dev"
  }
}
// https://registry.terraform.io/modules/terraform-aws-modules/vpc/aws/2.21.0

module "eks" {
  source       = "terraform-aws-modules/eks/aws"
  version      = "7.0.1"
  cluster_name = var.cluster_name_eks
  subnets      = module.vpc.private_subnets
  vpc_id       = module.vpc.vpc_id
  cluster_version = var.cluster_version_eks
  worker_groups = [
    {
      instance_type = var.instance_type_eks
      asg_max_size  = var.asg_max_size_eks
    }
  ]

}

// https://registry.terraform.io/modules/terraform-aws-modules/eks/aws/7.0.1
