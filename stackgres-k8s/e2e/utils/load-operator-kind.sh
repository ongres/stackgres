if [ ! -z "$REUSE_OPERATOR" ]
then
  exit 0
fi

export KUBECONFIG="$(kind get kubeconfig-path --name "$KIND_NAME")"

mvn -q clean package -P build-image-jvm
kind load docker-image --name "$KIND_NAME" "$IMAGE_NAME"

