provider "google" {
  credentials = file("../../../../../mygcp/stackgres-demo-256115-60f818b1950a.json")
  project     = var.project
  region      = var.region
}

// Module GCP
module "gcp" {
source                  ="./modules/cloud/gcp"
project_vpc          = "stackgres-demo-256115"
network_name            = "vpc-01"
subnet_name             = "gke-subnet"
subnet_ip               = "10.0.0.0/17"
subnet_region           = "us-central1"
range_name_pods         = "us-central1-01-gke-01-pods"
ip_cidr_range_pods      = "192.168.0.0/18"
range_name_services     = "us-central1-01-gke-01-services"
ip_cidr_range_services  = "192.168.64.0/18"
project_gke          = var.project
name_gke                = "gke-test-1"
region_gke              = "us-central1"
zones_gke               = ["us-central1-a", "us-central1-b", "us-central1-f"]
name_np                 = "default-node-pool"
machine_type_np         = "n1-standard-2"
min_count_np            = "1"
max_count_np            = "3"
disk_size_gb_np         = "20"
service_account_np      = "stackgres-demo@stackgres-demo-256115.iam.gserviceaccount.com"
initial_node_count_np   = "1"
value_np                = "my-node-pool"
}
