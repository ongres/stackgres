module "vpc" {
    source  = "terraform-google-modules/network/google"
    version = "~> 1.0.0"
    project_id   = var.project_vpc
    network_name = var.network_name
    routing_mode = "GLOBAL"

    subnets = [
        {
            subnet_name           = var.subnet_name
            subnet_ip             = var.subnet_ip
            subnet_region         = var.subnet_region
        },
     ]
    secondary_ranges = {
        gke-subnet = [
            {
                range_name    = var.range_name_pods
                ip_cidr_range = var.ip_cidr_range_pods
                },
                {
              range_name    = var.range_name_services
              ip_cidr_range = var.ip_cidr_range_services
          }

        ]
    }
 }

// https://registry.terraform.io/modules/terraform-google-modules/network/google/2.0.0

  module "gke" {
  source                     = "terraform-google-modules/kubernetes-engine/google"
  version                    = "6.1.1"
  project_id                 = var.project_gke
  name                       = var.name_gke
  region                     = var.region_gke
  zones                      = var.zones_gke
  network                    = "${module.vpc.network_name}"
  subnetwork                 = var.subnet_name
  ip_range_pods              = var.range_name_pods
  ip_range_services          = var.range_name_services
  http_load_balancing        = false
  horizontal_pod_autoscaling = true
  network_policy             = true


  node_pools = [
    {
      name               = var.name_np
      machine_type       = var.machine_type_np
      min_count          = var.min_count_np
      max_count          = var.max_count_np
      local_ssd_count    = 0
      disk_size_gb       = var.disk_size_gb_np
      disk_type          = "pd-standard"
      image_type         = "COS"
      auto_repair        = true
      auto_upgrade       = true
      service_account    = var.service_account_np
      preemptible        = false
      initial_node_count = var.initial_node_count_np
    },
  ]

  node_pools_oauth_scopes = {
    all = []

    default-node-pool = [
      "https://www.googleapis.com/auth/cloud-platform",
    ]
  }

  node_pools_labels = {
    all = {}

    default-node-pool = {
      default-node-pool = true
    }
  }

  node_pools_metadata = {
    all = {}

    default-node-pool = {
      node-pool-metadata-custom-value = var.value_np
    }
  }

  node_pools_tags = {
    all = []

    default-node-pool = [
      "default-node-pool",
    ]
  }

}


// https://registry.terraform.io/modules/terraform-google-modules/kubernetes-engine/google/6.1.1
