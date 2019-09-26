SERVICE_NAME="stackgres-operator"
NAMESPACE="stackgres"

TEMP_DIRECTORY=$(mktemp -d)

cat <<EOF >> ${TEMP_DIRECTORY}/csr.conf
[req]
req_extensions = v3_req
distinguished_name = req_distinguished_name
[req_distinguished_name]
[ v3_req ]
basicConstraints = CA:FALSE
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = ${SERVICE_NAME}
DNS.2 = ${SERVICE_NAME}.${NAMESPACE}
DNS.3 = ${SERVICE_NAME}.${NAMESPACE}.svc
DNS.4 = ${SERVICE_NAME}.${NAMESPACE}.svc.cluster.local
EOF

openssl req -new -nodes -text -keyout server-key.pem \
    -subj "/CN=${SERVICE_NAME}.${NAMESPACE}.svc" \
    -out ${TEMP_DIRECTORY}/server.csr \
    -config ${TEMP_DIRECTORY}/csr.conf

cat <<EOF | kubectl apply -f -
apiVersion: certificates.k8s.io/v1beta1
kind: CertificateSigningRequest
metadata:
  name: ${SERVICE_NAME}.${NAMESPACE}
spec:
  request: $(cat ${TEMP_DIRECTORY}/server.csr | base64 | tr -d '\n')
  usages:
  - digital signature
  - key encipherment
  - server auth
EOF
sleep 10

kubectl certificate approve ${SERVICE_NAME}.${NAMESPACE}
sleep 3

CA_BUNDLE=$(kubectl get csr ${SERVICE_NAME}.${NAMESPACE} -o jsonpath='{.status.certificate}')

echo ${CA_BUNDLE} | openssl base64 -d -A -out server.crt

sed -i "s/  caBundle:.*/  caBundle: ${CA_BUNDLE}/g" operator/install/kubernetes/chart/stackgres-operator/values.yaml

docker cp server.crt kind-control-plane:/usr/local/share/ca-certificates/validator.crt
docker exec -it kind-control-plane sh -c "update-ca-certificates"
