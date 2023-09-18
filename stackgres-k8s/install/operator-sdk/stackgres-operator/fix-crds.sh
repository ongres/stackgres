#!/bin/sh

set -xe

for CRD in bundle/manifests/stackgres.io_*
do
  cp "config/crd/bases/$(yq -r .spec.names.kind "$CRD").yaml" "$CRD"
  CRD_NAME="$(yq -r '.metadata.name' "$CRD")"
  CRD_SINGULAR="$(yq -r .spec.names.singular "$CRD")"
  yq -y '.spec.webhookdefinitions = (.spec.webhookdefinitions 
    | map(
      if .type == "ConversionWebhook" and .conversionCRDs[0] == "'"$CRD_NAME"'"
        then .webhookPath = "/stackgres/conversion/'"$CRD_SINGULAR"'" else . end
      )
    )' bundle/manifests/stackgres.clusterserviceversion.yaml > bundle/manifests/stackgres.clusterserviceversion.yaml.tmp
  mv bundle/manifests/stackgres.clusterserviceversion.yaml.tmp bundle/manifests/stackgres.clusterserviceversion.yaml
done
