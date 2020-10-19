#!/bin/sh

set -e

BASE_PATH="$(dirname "$0")"
INFO_PATH="$BASE_PATH/Info.yaml"
SCHEMAS_PATH="$BASE_PATH/schemas"
PATHS_PATH="$BASE_PATH/paths"
APIWEB_PATH="$BASE_PATH/../../.."
SWAGGER_YAML_FILE="$APIWEB_PATH/target/swagger.yaml"
SWAGGER_JSON_FILE="$APIWEB_PATH/target/swagger.json"
MERGED_SWAGGER_YAML_FILE="$APIWEB_PATH/target/swagger-merged.yaml"
MERGED_SWAGGER_JSON_FILE="$APIWEB_PATH/target/swagger-merged.json"
STACKGRES_K8S_PATH="$APIWEB_PATH/../.."
CRDS_PATH="$STACKGRES_K8S_PATH/install/helm/stackgres-operator/crds"

yq -y -s '.[0] * .[1]' "$SWAGGER_YAML_FILE" "$INFO_PATH" > "$MERGED_SWAGGER_YAML_FILE"

for PATH_PATH in "$PATHS_PATH"/*.yaml
do
  if [ "$(yq -r '.paths == null' "$PATH_PATH")" = "true" ]
  then
    echo "Field .paths not specified for $PATH_PATH"
    exit 1
  fi
  echo "Merging paths from ${PATH_PATH##*/}"
  cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
  yq -y -s ".[0] * .[1]" \
    "$MERGED_SWAGGER_YAML_FILE.tmp" "$PATH_PATH" > "$MERGED_SWAGGER_YAML_FILE"
  rm "$MERGED_SWAGGER_YAML_FILE.tmp"
done

for SCHEMA_PATH in "$SCHEMAS_PATH"/*.yaml
do
  if [ "$(yq -r '.type == null' "$SCHEMA_PATH")" = "true" ]
  then
    echo "Field .type not specified for $SCHEMA_PATH"
    exit 1
  fi
  TYPE="$(yq -r '.type' "$SCHEMA_PATH")"
  echo "Merging type $TYPE from ${SCHEMA_PATH##*/}"
  cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
  if [ "$(yq -r '.crdFile != null' "$SCHEMA_PATH")" = "true" ]
  then
    CRD_FILE="$(yq -r '.crdFile' "$SCHEMA_PATH")"
    yq -y -s ".[0] * { \"components\": { \"schemas\" : { \"$TYPE\": ({ "schema": { \"$TYPE\": .[1].spec.validation.openAPIV3Schema } } * .[2]).schema.$TYPE } } }" \
      "$MERGED_SWAGGER_YAML_FILE.tmp" "$CRDS_PATH/$CRD_FILE" "$SCHEMA_PATH" > "$MERGED_SWAGGER_YAML_FILE"
  else
    if [ "$(yq -r '.schema == null' "$SCHEMA_PATH")" = "true" ]
    then
      echo "Field .schema not specified for $SCHEMA_PATH"
      exit 1
    fi
    yq -y -s ".[0] * { \"components\": { \"schemas\" : { \"$TYPE\": .[1].schema.$TYPE } } }" \
      "$MERGED_SWAGGER_YAML_FILE.tmp" "$SCHEMA_PATH" > "$MERGED_SWAGGER_YAML_FILE"
  fi
  rm "$MERGED_SWAGGER_YAML_FILE.tmp"
done

KNOWN_TYPES=" $(for SCHEMA_PATH in "$SCHEMAS_PATH"/*.yaml
  do
    yq -r '.type' "$SCHEMA_PATH"
  done | tr '\n' ' ') "
for TYPE in $(yq -r '.components.schemas|keys|.[]' "$MERGED_SWAGGER_YAML_FILE")
do
  if ! cat << EOF | grep -qF " $TYPE "
$KNOWN_TYPES
EOF
  then
    echo "Removing orphan type $TYPE"
    cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
    yq -y "del(.components.schemas[\"$TYPE\"])" \
      "$MERGED_SWAGGER_YAML_FILE.tmp" \
      | sed "/^\s\+\$ref: '#\/components\/schemas\/$TYPE'$/d" \
      > "$MERGED_SWAGGER_YAML_FILE"
    rm "$MERGED_SWAGGER_YAML_FILE.tmp"
  fi
done

if [ "$(yq -r '. as $o|paths|select(.[0] == "paths" and .[(length - 1)] == "$ref")|. as $a|$o|getpath($a)|split("/")|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq)" != \
  "$(yq -r '. as $o|paths|select(.[0] == "components" and .[1] == "schemas" and (.|length) == 3)|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq)" ]
then
  echo "Some types are missing, please add them to the stackgres-k8s/src/api-web/src/main/swagger folder."
  echo
  echo "Required types:"
  echo
  yq -r '. as $o|paths|select(.[0] == "paths" and .[(length - 1)] == "$ref")|. as $a|$o|getpath($a)|split("/")|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq
  echo
  echo "Defined types:"
  echo
  yq -r '. as $o|paths|select(.[0] == "components" and .[1] == "schemas" and (.|length) == 3)|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq
  echo
  exit 1
fi

yq . "$MERGED_SWAGGER_YAML_FILE" > "$MERGED_SWAGGER_JSON_FILE"
