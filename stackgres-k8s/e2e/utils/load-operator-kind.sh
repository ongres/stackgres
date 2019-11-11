export KUBECONFIG="$(kind get kubeconfig-path --name="kind")"

if [ ! -z "$REUSE_OPERATOR" ]
then
  exit 0
fi

mvn clean package
$STACKGRES_PATH/src/operator/src/main/buildah/build-image-jvm.sh  > tmp.log
IMAGEID=$(tail -1 tmp.log)
rm -f tmp.log

buildah push $IMAGEID docker-daemon:stackgres/operator:development-jvm
kind load docker-image stackgres/operator:development-jvm

