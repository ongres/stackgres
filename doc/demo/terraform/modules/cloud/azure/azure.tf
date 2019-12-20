resource "azurerm_resource_group" "example" {
  name     = var.g_name_az
  location = var.g_location_az
}

// https://www.terraform.io/docs/providers/azurerm/r/resource_group.html

resource "azurerm_kubernetes_cluster" "example" {
  name                = var.name_az
  location            = azurerm_resource_group.example.location
  resource_group_name = azurerm_resource_group.example.name
  dns_prefix          = var.dns_prefix_az
  kubernetes_version  =var.kubernetes_version_az
  default_node_pool {
    name       = var.name_np
    node_count = var.node_count_np
    vm_size    = var.vm_size_az
    os_disk_size_gb = var.os_disk_size_gb_np
    
  }

  service_principal {
    client_id     = var.client_id
    client_secret = var.client_secret
  }

  tags = {
    Environment = "Production"
  }
}

// https://www.terraform.io/docs/providers/azurerm/r/kubernetes_cluster.html
