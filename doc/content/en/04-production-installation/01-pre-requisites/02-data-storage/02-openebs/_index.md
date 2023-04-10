---
title: Using OpenEBS
weight: 2
url: install/prerequisites/storage/openebs
description: "Details about how to install OpenEBS and how to configure it"
---

[OpenEBS](https://docs.openebs.io/docs/next/overview.html) turns any storage available on the Kubernetes worker nodes into local or distributed Kubernetes Persistent Volumes.

OpenEBS Local and Distributed volumes are implemented using a collection of OpenEBS Data Engines. OpenEBS Control Plane integrates deeply into Kubernetes and uses Kubernetes to manage the provisioning, scheduling and maintenance of OpenEBS Volumes.


## Add OpenEBS to Kubernetes Cluster

Only the basics steps to install and start to use OpenEBS will be shown, if you want to customize your installation, check the full documentation [here](https://docs.openebs.io/docs/next/quickstart.html#how-to-setup-and-use-openebs).

### Install OpenEBS using Helm

```
helm repo add openebs https://openebs.github.io/charts
helm repo update
helm install openebs --namespace openebs openebs/openebs --create-namespace
```

The helm chart will install the OpenEBS operator on the namespace `openebs`:

```
kubectl get pods -n openebs
NAME                                           READY   STATUS    RESTARTS   AGE
openebs-localpv-provisioner-6f686f7697-q4htl   1/1     Running   0          59m
openebs-ndm-4d7lj                              1/1     Running   0          59m
openebs-ndm-operator-5948569558-fmg8w          1/1     Running   0          59m
openebs-ndm-vnjgp                              1/1     Running   0          59m
openebs-ndm-wznp6                              1/1     Running   0          59m
```

And also will add two new StorageClasses:

```
kubectl get storageclasses.storage.k8s.io -l "app.kubernetes.io/managed-by=Helm"
NAME               PROVISIONER        RECLAIMPOLICY   VOLUMEBINDINGMODE      ALLOWVOLUMEEXPANSION   AGE
openebs-device     openebs.io/local   Delete          WaitForFirstConsumer   false                  80m
openebs-hostpath   openebs.io/local   Delete          WaitForFirstConsumer   false                  80m
```

There are 2 ways to use OpenEBS Local PV.

- **openebs-hostpath:** This option will create Kubernetes Persistent Volumes that will store the data into OS host path directory at: `/var/openebs/<"postgresql-pv-name">/.` Select this option, if you don’t have any additional block devices attached to Kubernetes nodes.

- **openebs-device:**  This option will create Kubernetes Local PVs using the block devices attached to the node. Select this option when you want to dedicate a complete block device on a node to a StackGres PostgreSQL application pod.

## Check the available storage devices

To check the available devices execute:

```
kubectl get blockdevices.openebs.io -n openebs

NAME                    NODENAME                                    SIZE           CLAIMSTATE   STATUS     AGE
blockdevice-8ba90b98... stackgres-demo-default-pool-287e7633-r3l3   402653184000   Unclaimed    Active     108m
blockdevice-9a78e6e3... stackgres-demo-default-pool-287e7633-r3l3   402653184000   Unclaimed    Active     108m
blockdevice-b773d8c7... stackgres-demo-default-pool-85077fde-5mh3   402653184000   Unclaimed    Active     108m
blockdevice-eda267ed... stackgres-demo-default-pool-7350ae65-kb6w   402653184000   Unclaimed    Active     108m
```

>**Note:** You can customize which devices will be discovered and managed by OpenEBS using the instructions [here](https://docs.openebs.io/docs/next/uglocalpv-device.html#optional-block-device-tagging).

Now you'll be ready to create your StackGres cluster. Do not forget using a OpenEBS Storage Class according to your use case when you create a cluster, check the required parameters in [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}})

After you create your cluster you can check that the storage devices were claimed:

```
kubectl get blockdevices.openebs.io -n openebs

NAME                    NODENAME                                    SIZE           CLAIMSTATE   STATUS     AGE
blockdevice-8ba90b98... stackgres-demo-default-pool-287e7633-r3l3   402653184000   Claimed      Active     120m
blockdevice-9a78e6e3... stackgres-demo-default-pool-287e7633-r3l3   402653184000   Claimed      Active     120m
blockdevice-b773d8c7... stackgres-demo-default-pool-85077fde-5mh3   402653184000   Claimed      Active     120m
blockdevice-eda267ed... stackgres-demo-default-pool-7350ae65-kb6w   402653184000   Claimed      Active     120m
```

Check a full installation demo [here](https://docs.openebs.io/docs/next/postgres.html).

