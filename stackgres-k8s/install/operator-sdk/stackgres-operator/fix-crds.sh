#!/bin/sh

set -xe

for CRD in bundle/manifests/stackgres.io_*
do
  cp "config/crd/bases/$(yq -r .spec.names.kind "$CRD").yaml" "$CRD"
  sed -i '/caBundle:/d' "$CRD"
  CRD_NAME="$(yq -r '.metadata.name' "$CRD")"
  CRD_SINGULAR="$(yq -r .spec.names.singular "$CRD")"
  if [ "$CREATE_CONVERSION_WEBHOOKS" = true ]
  then
    if [ "$CRD_SINGULAR" = sgconfig ]
    then
      yq -y '.spec.webhookdefinitions = (.spec.webhookdefinitions 
        | map(select((.type == "ConversionWebhook" and .conversionCRDs[0] == "'"$CRD_NAME"'") | not))
        )' bundle/manifests/stackgres.clusterserviceversion.yaml \
        > bundle/manifests/stackgres.clusterserviceversion.yaml.tmp
      mv bundle/manifests/stackgres.clusterserviceversion.yaml.tmp \
        bundle/manifests/stackgres.clusterserviceversion.yaml
      yq -y 'del(.spec.conversion)' "$CRD" > "$CRD".tmp
      mv "$CRD".tmp "$CRD"
    else
      yq -y '.spec.webhookdefinitions = (.spec.webhookdefinitions 
        | map(
          if .type == "ConversionWebhook" and .conversionCRDs[0] == "'"$CRD_NAME"'"
            then .webhookPath = "/stackgres/conversion/'"$CRD_SINGULAR"'" else . end
          )
        )' bundle/manifests/stackgres.clusterserviceversion.yaml \
        > bundle/manifests/stackgres.clusterserviceversion.yaml.tmp
      mv bundle/manifests/stackgres.clusterserviceversion.yaml.tmp \
        bundle/manifests/stackgres.clusterserviceversion.yaml
    fi
  fi
done
yq -s -y '.[0] as $config | .[1] as $bundle | $bundle | .spec.relatedImages = $config.spec.relatedImages' \
  config/manifests/bases/stackgres.clusterserviceversion.yaml \
  bundle/manifests/stackgres.clusterserviceversion.yaml \
  > bundle/manifests/stackgres.clusterserviceversion.yaml.tmp
mv bundle/manifests/stackgres.clusterserviceversion.yaml.tmp \
  bundle/manifests/stackgres.clusterserviceversion.yaml
