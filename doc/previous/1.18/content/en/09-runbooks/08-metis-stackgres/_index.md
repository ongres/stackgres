---
title: Run Metis on top of StackGres
weight: 8
url: /runbooks/metis-stackgres
description: How to run Metis on top of Stackgres
showToc: true
---

In this how-to guide, we'll walk you through the whole process of setting up [Metis](https://www.metisdata.io/) on a Kubernetes cluster using [StackGres](https://stackgres.io/). This guide is also available as a [video](https://www.youtube.com/watch?v=neB0JH7hfjE)

> This post has been published with explicit permission from Metis. It's a copy of [an article](https://www.metisdata.io/blog/how-to-run-metis-on-top-of-stackgres) originally published on Metis Data's website on **February 12, 2024**.


Setting Up Metis On Top Of StackGres
------------------------------------

We’ll need to install the Kubernetes cluster, then install StackGres, then configure the StackGres cluster, and then configure Metis. Instructions below are for Amazon Linux 2 x86\_64 but will work the same way on other operating systems like Mac OS and Windows (with slight syntax changes for PowerShell or other shells), and on ARM64 architecture.

## Installing Kubernetes With Minikube

Before installing StackGres, you will need a running Kubernetes cluster and the usual command line tools [kubectl](https://kubernetes.io/docs/tasks/tools/) and [Helm](https://helm.sh/docs/intro/install/). Please refer to the respective installation pages if you don't have these tools. As for Kubernetes, if you don't have one you can try easily with [minikube](https://minikube.sigs.k8s.io/docs/start/). It can be installed like this:

```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
```


```bash
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```


This should give you a running single-node cluster in seconds (depending on your Internet connection speed). You can install it in other operating systems the way it’s described [in the docs](https://minikube.sigs.k8s.io/docs/start/).

Keep in mind that minikube uses Docker behind the scenes. It works natively in Linux, and with a virtual machine in MacOS and Windows. Refer to installation instructions for [Docker Desktop](https://www.docker.com/products/docker-desktop/) to learn how to install it on your machine.

You can now create the minikube cluster with:

This should give you the output similar to this:

```bash
* minikube v1.32.0 on Amazon 2
* Automatically selected the docker driver. Other choices: none, ssh
* Using Docker driver with root privileges
* Starting control plane node minikube in cluster minikube
* Pulling base image ...
* Creating docker container (CPUs=2, Memory=3900MB) ...
* Preparing Kubernetes v1.28.3 on Docker 24.0.7 ...
  - Generating certificates and keys ...
  - Booting up control plane ...
  - Configuring RBAC rules ...
* Configuring bridge CNI (Container Networking Interface) ...
* Verifying Kubernetes components...
  - Using image gcr.io/k8s-minikube/storage-provisioner:v5
* Enabled addons: storage-provisioner, default-storageclass
* Done! kubectl is now configured to use "minikube" cluster and "default" namespace by default
```


When you’re done with the examples, you can easily delete the cluster with:

Let’s now install StackGres.

## Installing StackGres

The best way to install StackGres is through the official Helm chart. You can install it in this way:

```bash
curl -LO https://get.helm.sh/helm-v3.14.0-linux-amd64.tar.gz
```


```bash
tar -zxvf helm-v3.14.0-linux-amd64.tar.gz
```


```bash
sudo mv linux-amd64/helm /usr/local/bin/helm
```


Follow [this page](https://helm.sh/docs/intro/install/) to install Helm on your machine.

For our particular setup, we use the following Helm commands:

```bash
helm repo add stackgres-charts https://stackgres.io/downloads/stackgres-k8s/stackgres/helm/
```


```bash
helm install --create-namespace --namespace stackgres stackgres-operator stackgres-charts/stackgres-operator
```


To confirm that the operator is running while also waiting for setup to complete, run the following commands:

```bash
kubectl wait -n stackgres deployment -l group=stackgres.io --for=condition=Available
```


```bash
kubectl get pods -n stackgres -l group=stackgres.io
```


As you run the first **_kubectl_** command, it should wait for the successful deployment, and the second command will list the pods running in the **_stackgres_** namespace.

```bash
NAME                                READY   STATUS    RESTARTS        AGE
stackgres-operator-57bff75d-xlnfp   1/1     Running   1 (2m36s ago)   2m58s
```


## Creating a StackGres Cluster

Here, we'll create an SGCluster configured to fit Metis requirements. We’re going to use some resources created inline. Metis requires the [pg\_stat\_statements](https://www.postgresql.org/docs/current/pgstatstatements.html) extension, a separate user, and permissions to monitor databases.

Let’s create a password for the user that will get initialized based on the SQL command:

```bash
kubectl -n stackgres create secret generic metis-user-password-secret --from-literal=metis-create-user-sql="create user metis password 'admin123'"
```


Let’s now create a script that will create a database named **_metis_** and a user **_metis_**:

```yaml
apiVersion: stackgres.io/v1
kind: SGScript
metadata:
  namespace: stackgres
  name: cluster-scripts
spec:
  scripts:
  - name: create-metis-user
    scriptFrom:
      secretKeyRef:
        name: metis-user-password-secret
        key: metis-create-user-sql
  - name: grant-monitor-to-metis
    script: |
            GRANT pg_monitor TO metis
  - name: create-metis-database
    script: |
            CREATE DATABASE metis OWNER metis
  - name: grant-conntent-to-metis-database
    script: |
            GRANT CONNECT ON DATABASE metis TO metis
  - name: create-pgstatstatements-extension
    database: metis
    script: |
            CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```


We can see the script has five parts. First, we create the user with a password and store the password in the Kubernetes secret. Next, we give **_pg\_monitor_** permissions to the user. Next, we create the database. Then, we let the user connect to the database. Finally, we create the **_pg\_stat\_statements_** extension in the **_metis_** database.

We are now ready to create the Postgres cluster:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: stackgres
  name: cluster
spec:
  postgres:
    version: '15.0'
  instances: 1
  pods:
    persistentVolume:
      size: '5Gi'
  managedSql:
    scripts:
    - sgScript: cluster-scripts
```


It should take a few seconds to a few minutes for the cluster to be up and running:

```bash
kubectl get pods -n stackgres
```


```bash
NAME       READY   STATUS    RESTARTS AGE
cluster-0  6/6     Running   0        74s

```


Likewise, a database named **_metis_** must exist and be owned by the same user:

```bash
kubectl -n stackgres exec -it cluster-0 -c postgres-util -- psql -l metis
```


```bash
    	List of databases
   Name	|  Owner   | Encoding | Collate |  Ctype  | ICU Locale | Locale Provider |   Access privileges
--------+----------+----------+---------+---------+------------+-----------------+--------------------
 metis 	| metis    | UTF8     | C.UTF-8 | C.UTF-8 |            | libc            | =Tc/metis
 ...

```


## Deploying Metis

We can now add monitoring of the database to Metis. Go to [Metis](https://app.metisdata.io/monitoring) and click **_Deploy_**:

![Metis Deploy](metis-deploy.png "Deploy")

Select **_Postgres_** and click **_Next_**:

![Metis Select Vendor](metis-select-vendor.png "Select vendor")

Metis instructs us how to configure the user and grant necessary permissions. We already did that. Click **_Next_**.

![Metis Host Connection](metis-host-connection.png "Metis Host Connection")

Metis asks for the connection string now:

![Metis Configuration](metis-configuration.png "Metis Configuration")

Let’s extract the server name:

```bash
kubectl get svc -n stackgres
```


```bash
NAME                TYPE          CLUSTER-IP      EXTERNAL-IP                           PORT(S)             AGE
cluster             ClusterIP     10.99.23.208    <none>                                5432/TCP,5433/TCP   2m34s
cluster-config      ClusterIP     None            <none>                                <none>              2m34s
cluster-primary     ExternalName  <none>          cluster.stackgres.svc.cluster.local   <none>              2m34s
cluster-replicas    ClusterIP     10.103.149.23   <none>                                5432/TCP,5433/TCP   2m34s
cluster-rest        ClusterIP     10.104.238.166  <none>                                8008/TCP            2m34s
stackgres-operator  ClusterIP     10.108.192.220  <none>                                443/TCP             34m
stackgres-restapi   ClusterIP     10.103.146.192  <none>                                443/TCP             33m
```


The service name is **cluster.stackgres.svc.cluster.local**. The final URL should be like this:

```bash
postgresql://metis:admin123@cluster.stackgres.svc.cluster.local:5432/postgres?sslmode=disable
```


![Metis Connection String](metis-connection-string.png "Metis Connection String")

Click **_Next_**. Finally, click on **_Helm_** and copy the script to deploy Metis.

![Metis Helm](metis-helm.png "Metis Helm")

Extend the script with **_\--namespace stackgres_** and run it. Your API key will be different as its a specific API key to be used with your project:

```bash
helm repo add metis https://charts.metisdata.io
```


```bash
helm install mmc metis/mmc-chart --namespace stackgres --set API_KEY=YOURAPIKEY --set-json CONNECTION_STRING='[{ "uri":"postgresql://metis:admin123@cluster.stackgres.svc.cluster.local:5432/metis?sslmode=disable"  }]'
```


When you execute the script, you should get the following:

```bash
NAME: mmc
LAST DEPLOYED: Tue Jan 23 11:24:14 2024
NAMESPACE: stackgres
STATUS: deployed
REVISION: 1
TEST SUITE: None

```


We can verify that it works with:

```bash
kubectl get pods -n stackgres
```


```bash
NAME                             	READY   STATUS	RESTARTS  	AGE
cluster-0                        	6/6 	Running   0         	25m
mmc-deployment-58dbb76fcc-vnnd5  	1/1 	Running   0         	13s
```


You can now go to [Metis Monitoring](https://app.metisdata.io/monitoring) and see that the new host has been added:

![Metis Monitoring](metis-monitoring.png "Metis Monitoring")

When you click on the host, you should see the properties and databases:

![Metis Database](metis-databases.png "Metis Database")

You can also click on database **_metis_** and get all the details:

![Metis Observability](metis-observability.png "Metis Observability")

Notice that many charts are empty as it’s a completely new database.

Monitoring Live Database Activity
---------------------------------

Let’s now add some data and run some queries in the database.

```bash
kubectl -n stackgres exec -it cluster-0 -c postgres-util -- psql --username=metis --dbname=metis -c "CREATE TABLE orders(region VARCHAR(100), amount INT, product VARCHAR(100))"
```


```bash
kubectl -n stackgres exec -it cluster-0 -c postgres-util -- psql --username=metis --dbname=metis -c "INSERT INTO orders(region, amount, product)VALUES ('EU', 10, 'Product1'), ('EU', 20, 'Product2'), ('US', 1, 'XYZ'), ('JP', 10, 'ABC')"
```


```bash
kubectl -n stackgres exec -it cluster-0 -c postgres-util -- psql --username=metis --dbname=metis -c "SELECT region, product, (SELECT SUM(amount) AS total_sales FROM orders AS o2 WHERE o2.region = o.region GROUP BY region)
FROM orders AS o
WHERE region IN (
    SELECT region
    FROM orders
    GROUP BY region
    HAVING SUM(amount) > (
   	 SELECT SUM(amount)
   	 FROM orders
    ) / 10
)"

```


We create table **_orders_**, insert some data, and then run a query that extracts the rows with some aggregation. This is the output:

```bash
region  | product  | total_sales
--------+----------+-------------
 EU     | Product1 |        30
 EU     | Product2 |        30
 JP     | ABC      |        10
(3 rows)
```


You can now go to Metis and check the **_Table Sizes_** widget. It shows the **_orders_** table:

![Metis Table Sizes](metis-table-sizes.png "Metis Table Sizes")

You can also go to **_Top Queries_** widget and notice that new queries are flowing to the database:

![Metis Top Queries](metis-top-queries.png "Metis Top Queries")

Metis captures the live activity of your database and can suggest how to improve things.

*   [Metis Discord](https://metisdata.io/metis-discord)