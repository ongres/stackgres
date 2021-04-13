---
title: StackGres Operator Install
weight: 1
url: install/integrations/nutanix-karbon/sgoperator-install
---

# StackGres Operator Install

The StackGres Operator deployment will run simple commands from the automation process, this is our GitOps for StackGres.

## Installation Steps

Once the Karbon Cluster is ready, start creating the required resources to deploy the StackGres operator as follows.

StackGres (the operator and associated components) may be installed on any namespace. It is recommended to create a dedicated namespace for StackGres:
```sh
kubectl create namespace stackgres
```

And we should created the namespace where we want to run our clusters

```sh
kubectl create namespace karbon
```

StackGres recommended installation is performed from the published Helm chart. The following command will install StackGres with Helm3, allow StackGres Web Console, and exposing that Web Console via a load balancer

```sh
helm install --namespace stackgres stackgres-operator \
--set-string adminui.service.type=LoadBalancer \
https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.4/helm/stackgres-operator.tgz
```

Please refer to [Helm chart parameters]({{% relref "04-production-installation/06-cluster-parameters" %}}) for further
customization of the above Helm parameters.
Add or Replace them for your custom installation parameters, if needed.

Note that using `adminui.service.type=LoadBalancer` will create a network load balancer. You may alternatively use `ClusterIP` if that's your preference.

StackGres installation may take a few minutes. The output will be similar to:

```plain
NAME: stackgres-operator
LAST DEPLOYED: Mon Mar  1 00:25:10 2021
NAMESPACE: stackgres
STATUS: deployed
REVISION: 1
TEST SUITE: None
NOTES:
Release Name: stackgres-operator
StackGres Version: 0.9.4

   _____ _             _     _____
  / ____| |           | |   / ____|
 | (___ | |_ __ _  ___| | _| |  __ _ __ ___  ___
  \___ \| __/ _` |/ __| |/ / | |_ | '__/ _ \/ __|
  ____) | || (_| | (__|   <| |__| | | |  __/\__ \
 |_____/ \__\__,_|\___|_|\_\\_____|_|  \___||___/
                                  by OnGres, Inc.

Check if the operator was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-operator

    kubectl wait -n stackgres deployment/stackgres-operator --for condition=Available

Check if the restapi was successfully deployed and is available:

    kubectl describe deployment -n stackgres stackgres-restapi

    kubectl wait -n stackgres deployment/stackgres-restapi --for condition=Available

To access StackGres Operator UI from localhost, run the below commands:

    POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")

    kubectl port-forward "$POD_NAME" 8443:9443 --namespace stackgres

Read more about port forwarding here: http://kubernetes.io/docs/user-guide/kubectl/kubectl_port-forward/

Now you can access the StackGres Operator UI on:

https://localhost:8443

To get the username, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'

To get the generated password, run the command:

    kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'

Remember to remove the generated password hint from the secret to avoid security flaws:

    kubectl patch secrets --namespace stackgres stackgres-restapi --type json -p '[{"op":"remove","path":"/data/clearPassword"}]'
```
Several useful commands are provided as part of the Helm installation output. Let's use them to connect to the StackGres Web Console.
Get user and password and save it to use later:

```sh
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\n" (.data.k8sUsername | base64decode) }}'

kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "password = %s\n" (.data.clearPassword | base64decode) }}'
```

If you are working in a Karbon Laboratory Cluster and connecting to Karbon through a Jumper host, forwarding the node IP where the StackGres RestApi is running is needed. Running the next command from the Jumper Host will forward the rest api IP to itself.

```sh
POD_NAME=$(kubectl get pods --namespace stackgres -l "app=stackgres-restapi" -o jsonpath="{.items[0].metadata.name}")

kubectl port-forward “$POD_NAME” 8443:9443 --namespace stackgres
```

If the Jumper Host doesn’t contain a graphical interface, you should create a ssh tunnel to get access in SG UI. Open another terminal and run the following command (don’t close the previous one which holds the kubernetes port forward):

```sh
ssh -L 8443:localhost:8443 [Jumper Host IP Address]
```

To access the web console paste the link https://localhost:8443 in the Citrix Instance’s Browser and you should see the SG login page.