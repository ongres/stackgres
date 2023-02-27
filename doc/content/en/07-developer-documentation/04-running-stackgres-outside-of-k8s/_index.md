---
title: Running StackGres Outside of K8s
weight: 4
url: developer/stackgres/outside
description: Details about how to run the operator outside of Kubernetes.
---

It is possible to run the StackGres operator or the StackGres REST API outside of the Kubernetes cluster, and to connect to these applications from the other Kubernetes resources.

This is helpful during the development process, especially with the possibility to run Quarkus in dev mode locally.

The following steps assume that you're using [kind](https://kind.sigs.k8s.io/).
If your setup differs, you might need to adapt the cluster address and make sure that the connection between the Kubernetes cluster and your local process works.

Once you have `kind` installed, you can set up a local cluster by running the E2E tests, or executing:

```bash
sh stackgres-k8s/e2e/e2e setup_k8s
```

## StackGres Operator

You can run the StackGres operator locally, while the other resources are deployed to Kubernetes as usual.

For this, you need to install the StackGres resources on the Kubernetes cluster, excluding the actual operator:

```bash
helm install stackgres-operator stackgres-k8s/install/helm/stackgres-operator \
  --create-namespace --namespace stackgres \
  --set deploy.operator=false \
  --set developer.externalOperatorIp=172.17.0.1 \
  --set developer.externalOperatorPort=8443 \
  --set-string cert.crt="$(base64 -w0 stackgres-k8s/src/operator/src/test/resources/certs/server.crt)" \
  --set-string cert.key="$(base64 -w0 stackgres-k8s/src/operator/src/test/resources/certs/server-key.pem)"
```

This will install StackGres excluding the StackGres operator deployment.
The `stackgres-operator` service will not point to a pod but instead to `172.17.0.1:8443` (served by the local Java process or dev mode we're about to start):

```bash
$ kubectl get deployments -n stackgres
NAME                READY   UP-TO-DATE   AVAILABLE   AGE
stackgres-restapi   1/1     1            1           4m29s

$ kubectl get services -n stackgres
NAME                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)   AGE
stackgres-operator   ClusterIP   10.96.117.126   <none>        443/TCP   4m48s
stackgres-restapi    ClusterIP   10.96.7.202     <none>        443/TCP   4m48s

$ kubectl get endpoints -n stackgres
NAME                 ENDPOINTS         AGE
stackgres-operator   172.17.0.1:8443   4m59s
stackgres-restapi    10.244.0.9:9443   4m59s
```

Then you can start the operator outside of Kubernetes.
First build the operator (see the [Building StackGres]({{% relref "07-developer-documentation/02-building-stackgres" %}}) section), and then start the Java process:

```bash
cd stackgres-k8s/src/
./mvnw clean install -DskipTests
cd operator/
java \
 -Dquarkus.http.ssl.certificate.files=$(pwd)/src/test/resources/certs/server.crt \
 -Dquarkus.http.ssl.certificate.key-files=$(pwd)/src/test/resources/certs/server-key.pem \
 -Dquarkus.http.ssl-port=8443 \
 -jar target/quarkus-app/quarkus-run.jar
```

### Development Mode

Alternatively, you can use Quarkus' dev mode:

```bash
cd stackgres-k8s/src/operator/
mvn \
 -Dquarkus.http.ssl.certificate.files=$(pwd)/src/test/resources/certs/server.crt \
 -Dquarkus.http.ssl.certificate.key-files=$(pwd)/src/test/resources/certs/server-key.pem \
 -Dquarkus.http.host=0.0.0.0 \
 -Dquarkus.http.ssl-port=8443 \
 quarkus:dev
```

The dev mode listens to Java's remote debug port (by default `localhost:5005`), to which you can connect your IDE to debug locally.

## StackGres REST API

You can run also run the StackGres REST API locally, while the other resources are deployed to Kubernetes as usual.

Assuming you have a clean Kubernetes setup again -- if not you can run `sh stackgres-k8s/e2e/e2e setup_k8s` to reset.

You can install the StackGres resources on the Kubernetes cluster, excluding the REST API:

```bash
helm install stackgres-operator stackgres-k8s/install/helm/stackgres-operator \
  --create-namespace --namespace stackgres \
  --set deploy.restapi=false \
  --set developer.externalRestApiIp=172.17.0.1 \
  --set developer.externalRestApiPort=8443 \
  --set-string cert.crt="$(base64 -w0 stackgres-k8s/src/operator/src/test/resources/certs/server.crt)" \
  --set-string cert.key="$(base64 -w0 stackgres-k8s/src/operator/src/test/resources/certs/server-key.pem)"
```

This will install StackGres excluding the StackGres REST API (api-web).
The `stackgres-restapi` service will not point to a pod but instead to `172.17.0.1:8443` (served by the local Java process or dev mode we're about to start):

```bash
$ kubectl get deployments -n stackgres
NAME                 READY   UP-TO-DATE   AVAILABLE   AGE
stackgres-operator   1/1     1            1           3m29s

$ kubectl get services -n stackgres
NAME                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)   AGE
stackgres-operator   ClusterIP   10.96.90.206    <none>        443/TCP   3m48s
stackgres-restapi    ClusterIP   10.96.248.40    <none>        443/TCP   3m48s

$ kubectl get endpoints -n stackgres
NAME                 ENDPOINTS         AGE
stackgres-operator   10.244.0.9:8443   3m59s
stackgres-restapi    172.17.0.1:8443   3m59s
```

Then you can start the REST API outside of Kubernetes.
First build the project (see the [Building StackGres]({{% relref "07-developer-documentation/02-building-stackgres" %}}) section), and then start the Java process:

```bash
cd stackgres-k8s/src/
./mvnw clean install -DskipTests
cd api-web/
java \
 -Dquarkus.http.ssl.certificate.files=$(pwd)/../operator/src/test/resources/certs/server.crt \
 -Dquarkus.http.ssl.certificate.key-files=$(pwd)/../operator/src/test/resources/certs/server-key.pem \
 -Dmp.jwt.verify.publickey.location=$(pwd)/src/test/resources/jwt/rsa_public.pem \
 -Dsmallrye.jwt.sign.key.location=$(pwd)/src/test/resources/jwt/rsa_private.key \
 -Dquarkus.http.ssl-port=8443 \
 -jar target/quarkus-app/quarkus-run.jar
```

Then you can access the REST API locally.
In order to query the StackGres-relevant data, you need to fetch an access token.
You can use the admin credentials to do that.

```bash
kubectl get secret -n stackgres stackgres-restapi --template '{{ printf "username = %s\npassword = %s\n" (.data.k8sUsername | base64decode) ( .data.clearPassword | base64decode) }}'
```

This secret has been created by the Helm chart and contains the admin password which we use to create a token:

```bash
token=$(curl -d '{ "username": "admin", "password": "<admin-password>" }' -k https://localhost:8443/stackgres/auth/login -H 'Content-Type: application/json' -H 'Accept: application/json' -s | jq -r .access_token)
curl -k https://localhost:8443/stackgres/sgclusters/ -H "Authorization: Bearer $token" -H 'Accept: application/json' -s 
```

The result of the first command saves the access token in a shell variable `$token`.
We then query the SGclusters using the REST API.

### Development Mode

Of course, you can use Quarkus' dev mode for the REST API as well:

```bash
cd stackgres-k8s/src/api-web/
mvn \
 -Dquarkus.http.ssl.certificate.files=$(pwd)/../operator/src/test/resources/certs/server.crt \
 -Dquarkus.http.ssl.certificate.key-files=$(pwd)/../operator/src/test/resources/certs/server-key.pem \
 -Dmp.jwt.verify.publickey.location=$(pwd)/src/test/resources/jwt/rsa_public.pem \
 -Dsmallrye.jwt.sign.key.location=$(pwd)/src/test/resources/jwt/rsa_private.key \
 -Dquarkus.http.host=0.0.0.0 \
 -Dquarkus.http.ssl-port=8443 \
 quarkus:dev
```

