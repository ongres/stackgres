kind create cluster
export KUBECONFIG="$(kind get kubeconfig-path --name="kind")"

SERVICE_NAME="stackgres-operator"
NAMESPACE="stackgres"
TEMP_DIRECTORY="tmp"
rm -rf $TEMP_DIRECTORY
mkdir $TEMP_DIRECTORY

./self-signed-certificates.sh


mvn clean test package
./.gitlab/buildah-stackgres-jvm.sh > tmp.log
IMAGEID=$(tail -1 tmp.log)
rm -f tmp.log

buildah push $IMAGEID docker-daemon:stackgres/operator:development-jvm

kind load docker-image stackgres/operator:development-jvm

helm init
sleep 10
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}'
sleep 60

CA_BUNDLE=$(kubectl get csr ${SERVICE_NAME}.${NAMESPACE} -o jsonpath='{.status.certificate}')
CA_BUNDLE_LINE=$(cat operator/install/kubernetes/chart/stackgres-operator/values.yaml | grep caBundle:)

sed -i "s/  caBundle:.*/  caBundle: ${CA_BUNDLE}/g" operator/install/kubernetes/chart/stackgres-operator/values.yaml

helm install --name stackgres-operator operator/install/kubernetes/chart/stackgres-operator/

kubectl create secret generic ${SERVICE_NAME}.${NAMESPACE} \
        --from-file=root.key=server-key.pem \
        --from-file=server.crt=server.crt \
        -n ${NAMESPACE}

sed -i "s/  caBundle:.*/${CA_BUNDLE_LINE}/g" operator/install/kubernetes/chart/stackgres-operator/values.yaml

rm server.crt
rm server-key.pem
