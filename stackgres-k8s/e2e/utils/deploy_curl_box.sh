
NAMESPACE="default"
if [ -z ${1+x} ]; then NAMESPACE=$1; fi

cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: busybox-curl
  namespace: $NAMESPACE
  labels:
    app: busybox-curl
spec:
  containers:
  - image: radial/busyboxplus:curl
    command:
      - sleep
      - "3600"
    imagePullPolicy: IfNotPresent
    name: busybox
  restartPolicy: Always
EOF