---
title: "OKE"
weight: 5
url: install/prerequisites/k8s/oke
description: Oracle Cloud Infrastructure Container Engine for Kubernetes (OKE) is a fully-managed, scalable, and highly available service that you can use to deploy your containerized applications to the cloud.
---

This section shows how to create a [Container Engine for Kubernetes](https://www.oracle.com/br/cloud-native/container-engine-kubernetes/) cluster.

Assuming that you already had created a [Virtual Cloud Network](https://docs.oracle.com/en-us/iaas/Content/Network/Concepts/landing.htm) with the [pre-requisites](https://docs.oracle.com/en-us/iaas/Content/ContEng/Concepts/contengprerequisites.htm) to create an OKE cluster, and that you have the [OCI-CLI](https://docs.oracle.com/en-us/iaas/Content/API/Concepts/cliconcepts.htm) configured, you can continue to create a cluster.
We use the following characteristics which you might change:

* Compartment: Select or create a compartment to allocate the deployment
* Cluster name: `stackgres`
* Kubernetes version: `v1.21.5`
* Node Shape: `VM.Standard.E4.Flex`
* OCPU per node: 1
* Memory per node: 8 GB
* Number of nodes: 3
* Disk size: 50 GB
* VCN with 3 different subnets: Kubernetes Endpoint Subnet; Load Balancer Subnet; Node Pool Subnet

> This is an example to create a OKE cluster into a single AD

Create the necessary environment variables and replace the values with your tenancy information:

```
export compartment_id=[compartment-OCID]
export vnc_id=[VNC-OCID]
export endpoint_subnet_id=[endpoint-subnet-OCID]
export lb_subnet_id=[loadbalancer-subnet-OCID]
export nodes_subnet_id=[nodes-subnet-OCID]
```

Create the Kubernetes Cluster:

```
oci ce cluster create \
 --compartment-id $compartment_id \
 --kubernetes-version v1.21.5 \
 --name stackgres \
 --vcn-id $vnc_id \
 --endpoint-subnet-id $endpoint_subnet_id \
 --service-lb-subnet-ids '["'$lb_subnet_id'"]' \
 --endpoint-public-ip-enabled true \
 --persistent-volume-freeform-tags '{"stackgres" : "OKE"}'
```

The output will be similar to this:

```
   {
  ""opc-work-request-id": "ocid1.clustersworkrequest.oc1.[OCI-Regions].aaaaaaaa2p26em5geexn...""
   }
```

After the Cluster creation, create the node pool for the Kubernetes worker nodes:

```
oci ce node-pool create \
 --cluster-id $(oci ce cluster list --compartment-id $compartment_id --name stackgres --lifecycle-state ACTIVE --query data[0].id --raw-output) \
 --compartment-id $compartment_id \
 --kubernetes-version v1.21.5 \
 --name Pool1 \
 --node-shape VM.Standard.E4.Flex \
 --node-shape-config '{"memoryInGBs": 8.0, "ocpus": 1.0}' \
 --node-image-id $(oci compute image list --operating-system 'Oracle Linux' --operating-system-version 7.9 --sort-by TIMECREATED --compartment-id $compartment_id --query data[1].id --raw-output) \
 --node-boot-volume-size-in-gbs 50 \
 --size 3 \
 --placement-configs '[{"availabilityDomain": "'$(oci iam availability-domain list --compartment-id $compartment_id --query data[0].name --raw-output)'", "subnetId": "'$nodes_subnet_id'"}]' 
```

The output will be similar to this:

```
   {
  "opc-work-request-id": "ocid1.clustersworkrequest.oc1.[OCI-Regions].aaaaaaaa2p26em5geexn..."
   }
```

> After the cluster provisioning, it is highly recommend to change the default Kubernetes storage class:

```
kubectl patch storageclass oci -p '{"metadata": {"annotations":{"storageclass.beta.kubernetes.io/is-default-class":"false"}}}'
kubectl patch storageclass oci-bv -p '{"metadata": {"annotations":{"storageclass.beta.kubernetes.io/is-default-class":"true"}}}'
```

To clean up the Kubernetes cluster you can issue following:

Delete the node pool:

```
oci ce node-pool delete \
 --node-pool-id $(oci ce node-pool list --cluster-id $(oci ce cluster list --compartment-id $compartment_id --name stackgres --lifecycle-state ACTIVE --query data[0].id --raw-output) --compartment-id $compartment_id --query data[0].id --raw-output) \
 --force
```

Delete the Kubernetes cluster:

```
oci ce cluster delete \
 --cluster-id $(oci ce cluster list --compartment-id $compartment_id --name stackgres --lifecycle-state ACTIVE --query data[0].id --raw-output) \
 --force
```

You may also want to clean up compute disks used by persistence volumes that may have been created:

> This code terminates all Block Volumes with the Free Form Tag {"stackgres":"OKE"}, if you had provisioned more than one cluster in the same compartment with the code above, this may delete all your PV data.

```
oci bv volume list \
 --compartment-id $compartment_id \
 --lifecycle-state AVAILABLE \
 --query 'data[?"freeform-tags".stackgres == '\''OKE'\''].id' \
  | jq -r .[] | xargs -r -n 1 -I % oci bv volume delete --volume-id % --force
```
