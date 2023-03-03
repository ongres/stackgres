---
title: Running StackGres outside of K8s
weight: 4
url: developer/stackgres/outside
description: Details about how to run the operator outside of Kubernetes.
---

It is possible to run the StackGres operator outside of the Kubernetes cluster, and to connect to that operator from the other Kubernetes resources.

For this, you need to install the StackGres resources on the Kubernetes cluster, excluding the actual operator:

```bash
helm install stackgres-operator stackgres-k8s/install/helm/stackgres-operator
  --namespace stackgres
  --set deploy.create=false
  --set-string cert.crt="$(base64 stackgres-k8s/src/test/resources/certs/server.crt)"
  --set-string cert.key="$(base64 stackgres-k8s/src/test/resources/certs/server-key.pem)"
```

Create a Kubernetes service and endpoints that points to your locally-running operator:

```bash
cat << 'EOF' | kubectl create -f -
---
kind: Service
apiVersion: v1
metadata:
  namespace: stackgres
  name: stackgres-operator
spec:
  ports:
   - port: 443
     targetPort: 8443
---
kind: Endpoints
apiVersion: v1
metadata:
  namespace: stackgres
  name: stackgres-operator
subsets:
 - addresses:
    - ip: 172.17.0.1
   ports:
    - port: 8443
EOF
```

This configuration assumes that your using [kind](https://kind.sigs.k8s.io/).
If your setup differs, you would need to adapt the address and make sure that the connection between the Kubernetes cluster and your local process works.

Then you can start the operator outside of Kubernetes.
First build the operator (see the [building stackgres]({{% relref "07-developer-documentation/02-building-stackgres" %}}) section), and then start the Java process:

```bash
java -cp stackgres-k8s/src/operator/target/stackgres-operator-runner.jar \
  -Dquarkus.http.ssl.certificate.file=stackgres-k8s/src/test/resources/certs/server.crt \
  -Dquarkus.http.ssl.certificate.key-file=src/test/resources/certs/server-key.pem
  -Dquarkus.http.port=8080
  -Dquarkus.http.ssl-port=8443
```
