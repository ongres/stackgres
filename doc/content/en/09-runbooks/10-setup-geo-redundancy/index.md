---
title: Setup geo redundancy
weight: 10
url: /rubooks/geo-redundancy
description: Create a secondary cluster in a different datacenter or region 
---

Geo-redundancy is the distribution of mission-critical components, across multiple data centers, which reside in different geographic locations.
Geo-redundancy acts as a safety net in case your primary site fails or in the event of a disaster or an outage that impacts an entire region.

The objective of this mechanism is to guarantee that the applications continue working in case of a data center failure and that there is no or minimal data loss in such a case. The main datacenter cluster is replicated by a secondary cluster also called standby cluster. 
All the applications have to be duplicated in all the data centers to be equal and receive the traffic and perform as expected in the event of a failover.

The DNS entry points play an essential part in the components, in case of a data center failure the DNS must change and point to the entry point of the DR data center where all the database standby clusters will become the primary databases allowing Read/Writes operations. The sync process before the failover will ensure the integrity of the database and minimize any possible data loss.


## Configuration

In order to create all the configuration needed you’ll need to ensure you have your k8s environments up and running in all the datacenter or different regions. 

The next configuration is a basic configuration to accomplish the GEO redundancy, for an advanced configuration deep dive into the documentation.

Also to perform the next steps you need to have connectivity to your k8s clusters and install the required tools: 

- [Helm 3](https://helm.sh/docs/intro/install/)
- [Kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)


The next two K8s clusters are a demo clusters to create all the configs:

```bash
NAME            LOCATION        MASTER_VERSION      MASTER_IP       STATUS
monitoring-dc2  europe-west1-c  1.27.8-gke.1067004  32.182.119.251  RUNNING
monitoring-dc1  us-central1-c   1.27.8-gke.1067004  33.170.164.123  RUNNING
```

## Install the StackGres Operator

Add the helm char repository:

```bash
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
helm repo update
```

Install the operator:

```bash
helm install --create-namespace --namespace stackgres stackgres-operator stackgres-charts/stackgres-operator
```

Check the rest-api was successfully deployed and is available executing the following command:

```bash
kubectl wait -n stackgres deployment/stackgres-restapi --for condition=Available
```

Once the condition is met, you’ll see two new pods in the stackgres namespace:

```bash
kubectl get pods -n stackgres
NAME                                 READY   STATUS    RESTARTS       AGE
stackgres-operator-c7b95dfc6-c8ntq   1/1     Running   1 (109m ago)   109m
stackgres-restapi-7bc6c87fcb-kmzjq   2/2     Running   0              108m
```

The above steps must be executed in all the k8s clusters.

Create the StackGres cluster on Main DC

Connect to the main k8s cluster and create a new namespace where the database cluster will be deployed:

```bash
kubectl create namespace my-db
```

The next step is to create the SGCluster with the next definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
 name: my-db
 namespace: my-db
spec:
 postgres:
   version: '16'
 instances: 3
 profile: production
 pods:
   persistentVolume:
     size: '10Gi'
 postgresServices:
   primary:
     type: LoadBalancer
```

The usage of the service type LoadBalancer is because we’ll use the external IP later in the second k8s cluster. 

You can check the cluster status with the next command:

```bash
kubectl exec -it -n my-db my-db-0 -c patroni -- patronictl list
```

You’ll get an output like:

```bash
+ Cluster: my-db (7345440823344099843) -----------+----+-----------+
| Member  | Host            | Role    | State     | TL | Lag in MB |
+---------+-----------------+---------+-----------+----+-----------+
| my-db-0 | 10.76.0.19:7433 | Leader  | running   |  1 |           |
| my-db-1 | 10.76.0.20:7433 | Replica | streaming |  1 |         0 |
| my-db-2 | 10.76.0.21:7433 | Replica | streaming |  1 |         0 |
+---------+-----------------+---------+-----------+----+-----------+
```

Now that you have your primary cluster up and running, you need to backup the secrets from the cluster.  Execute the following command to generate the yaml file with the secrets.

```bash
kubectl get secrets -n my-db my-db -o yaml >> my-db-secrets.yaml
```

>**Note:** Edit the file and update the secret name to my-db-origin


## Create the StackGres cluster on the Secondary DC

Once you installed the StackGres operator, you need to create the secrets from the primary instance:

First, create the required namespace:

```bash
kubectl create namespace my-db
```

Then create the secrets from the generated YAML file in the previous steps

```bash
kubectl apply -f my-db-secrets.yaml 
```

Now you are ready to create your secondary cluster. First, you need to know the public endpoint(In this example)  of your main database. You can get it by executing the following command:  

```bash
kubectl get svc -n my-db 
NAME             TYPE           CLUSTER-IP     EXTERNAL-IP                     PORT(S)                         AGE
my-db            LoadBalancer   10.32.6.68     34.69.89.185                    5432:30386/TCP,5433:31460/TCP   98m
```


The SGCluster definition is the same as your main cluster with some additional configurations:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
 name: my-db
 namespace: my-db
spec:
 postgres:
   version: '16'
 instances: 3
 pods:
   persistentVolume:
     size: '10Gi'
 postgresServices:
   primary:
     type: ClusterIP
 profile: production
 replicateFrom:
   instance:
     external:
       host: 34.69.89.185
       port: 5433
   users:
     superuser:
       username:
         name: my-db-origin
         key: superuser-username
       password:
         name: my-db-origin
         key: superuser-password
     replication:
       username:
         name: my-db-origin
         key: replication-username
       password:
         name: my-db-origin
         key: replication-password
     authenticator:
       username:
         name: my-db-origin
         key: authenticator-username
       password:
         name: my-db-origin
         key: authenticator-password
```


You can see that it was added the section `replicateFrom` with all the required values. Here we used the endpoint from the main cluster and the secrets.  Important to note that the port used is `5433` instead of `5432`. The reason is that traffic through port `5432` goes through PgBouncer and replication connections are not allowed there. 

Now you have configured your main database cluster with a secondary cluster in another datacenter or region. 

Main database cluster state:

```bash
kubectl exec -it -n my-db my-db-0 -c patroni -- patronictl list
+ Cluster: my-db (7345440823344099843) -----------+----+-----------+
| Member  | Host            | Role    | State     | TL | Lag in MB |
+---------+-----------------+---------+-----------+----+-----------+
| my-db-0 | 10.76.0.19:7433 | Leader  | running   |  1 |           |
| my-db-1 | 10.76.0.20:7433 | Replica | streaming |  1 |         0 |
| my-db-2 | 10.76.0.21:7433 | Replica | streaming |  1 |         0 |
+---------+-----------------+---------+-----------+----+-----------+
```

Secondary database cluster state:

```bash 
kubectl exec -it -n my-db my-db-0 -c patroni -- patronictl list
+ Cluster: my-db (7345440823344099843) ------+-----------+----+-----------+
| Member  | Host            | Role           | State     | TL | Lag in MB |
+---------+-----------------+----------------+-----------+----+-----------+
| my-db-0 | 10.44.0.18:7433 | Standby Leader | streaming |  1 |           |
| my-db-1 | 10.44.0.19:7433 | Replica        | streaming |  1 |         0 |
| my-db-2 | 10.44.0.20:7433 | Replica        | streaming |  1 |         0 |
```

## How to execute a switchover between DC

To perform a switchover to the secondary cluster you just need to first demote the old cluster. If the old cluster is an SGCluster you may for example set its instances
 to 0 and wait for the Pod to be removed.
After the old cluster is demoted promote the standby leader to be a leader. You can do this executing by modifying the SGCluster definition, removing or commenting the `replicateFrom` section:

```bash
kubectl patch sgclusters.stackgres.io -n my-db my-db --type merge -p 'spec: { replicateFrom: null }'
```

Now the standby leader becomes the leader node:

```bash
kubectl exec -it -n my-db my-db-0 -c patroni -- patronictl list
+ Cluster: my-db (7345440823344099843) -----------+----+-----------+
| Member  | Host            | Role    | State     | TL | Lag in MB |
+---------+-----------------+---------+-----------+----+-----------+
| my-db-0 | 10.44.0.18:7433 | Leader  | running   |  2 |           |
| my-db-1 | 10.44.0.19:7433 | Replica | streaming |  2 |         0 |
| my-db-2 | 10.44.0.20:7433 | Replica | streaming |  2 |         0 |
+---------+-----------------+---------+-----------+----+-----------+
```

### Bring up again the `DC1` as a Standby leader

To reconnect the DC1 cluster to the new leader it is necessary to edit the SGCluster configuration and add the `replicateFrom` section as you did it to configure the secondary DC. Make sure to set the correct endpoint to the new leader. 

Add the following section to the SGCluster configuration:

```yaml
 replicateFrom:
   instance:
     external:
       host: 34.76.15.119
       port: 5433
   users:
     superuser:
       username:
         name: my-db
         key: superuser-username
       password:
         name: my-db
         key: superuser-password
     replication:
       username:
         name: my-db
         key: replication-username
       password:
         name: my-db
         key: replication-password
     authenticator:
       username:
         name: my-db
         key: authenticator-username
       password:
         name: my-db
         key: authenticator-password
```


Once you add the configuration the old leader will now follow the new one on the `DC2`:

```bash 
kubectl exec -it -n my-db my-db-0 -c patroni -- patronictl list
+ Cluster: my-db (7345440823344099843) ------+-----------+----+-----------+
| Member  | Host            | Role           | State     | TL | Lag in MB |
+---------+-----------------+----------------+-----------+----+-----------+
| my-db-0 | 10.76.0.19:7433 | Standby Leader | streaming |  2 |           |
| my-db-1 | 10.76.0.20:7433 | Replica        | streaming |  2 |         0 |
| my-db-2 | 10.76.0.21:7433 | Replica        | streaming |  2 |         0 |
```
