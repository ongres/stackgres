---
title: Running StackGres outside of K8s
weight: 4
---

To run StackGres outside of kubernetes you will first need to install some required kubernetes resources:

```
helm install stackgres-k8s/install/helm/stackgres-operator
  --namespace stackgres
  --name stackgres-operator
  --set deploy.create=false
  --set-string cert.crt="$(base64 stackgres-k8s/src/test/resources/certs/server.crt)"
  --set-string cert.key="$(base64 stackgres-k8s/src/test/resources/certs/server-key.pem)"
```

You have also to create a service in order to allow admission web hooks to work from outside of kubernetes:

```
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

This configuration only works if you use kind.

Then you may start the operator outside of kubernetes using the following command (remember to build the
 operator first, see [building stackgres](../02-building-stackgres) section):

```
java -cp stackgres-k8s/src/operator/target/stackgres-operator-runner.jar \
  -Dquarkus.http.ssl.certificate.file=stackgres-k8s/src/test/resources/certs/server.crt \
  -Dquarkus.http.ssl.certificate.key-file=src/test/resources/certs/server-key.pem
  -Dquarkus.http.port=8080
  -Dquarkus.http.ssl-port=8443
```