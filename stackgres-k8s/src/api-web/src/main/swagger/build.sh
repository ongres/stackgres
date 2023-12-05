#!/bin/sh

set -e

BASE_PATH="$(dirname "$0")"
INFO_PATH="$BASE_PATH/Info.yaml"
SCHEMAS_PATH="$BASE_PATH/schemas"
PATHS_PATH="$BASE_PATH/paths"
APIWEB_PATH="$BASE_PATH/../../.."
SWAGGER_YAML_FILE="$APIWEB_PATH/target/openapi.yaml"
SWAGGER_JSON_FILE="$APIWEB_PATH/target/openapi.json"
MERGED_SWAGGER_YAML_FILE="$APIWEB_PATH/target/swagger-merged.yaml"
MERGED_SWAGGER_JSON_FILE="$APIWEB_PATH/target/swagger-merged.json"
STACKGRES_K8S_PATH="$APIWEB_PATH/../.."
CRDS_PATH="$STACKGRES_K8S_PATH/src/common/src/main/resources/crds"

DEBUG="$(echo $- | grep -q x && echo true || echo false)"

echo "Expanding swagger refs"
yq -y --argjson debug "$DEBUG" "$(cat << 'EOF'
  . as $in | [paths | select(.[0] == "components" and (. | length) == 3)] as $dtos
    | reduce $dtos[] as $dto ($in;
      . as $accumulator | (if $debug then [ "Expanded DTO", $dto ] | debug else . end)
        | $accumulator | [paths(. == "#/components/schemas/" + $dto[-1]) | select(.[0] == "components")] as $refs
        | reduce $refs[] as $ref ($accumulator;
          . as $ref_accumulator | (if $debug then [ "Expanded $ref", $ref[0:-1] ] | debug else . end) | $ref_accumulator
            | setpath($ref[0:-1]; ($ref_accumulator|getpath($dto)))
          )
      )
EOF
  )" "$SWAGGER_YAML_FILE" > "$MERGED_SWAGGER_YAML_FILE"
if ! yq '[paths | select(.[0] == "components" and .[-1] == "$ref")] | length' "$MERGED_SWAGGER_YAML_FILE" | grep -q '^0$'
then
  echo "Some $ref where not expanded:"
  echo
  yq -c '[paths | select(.[0] == "components" and .[-1] == "$ref")]' "$MERGED_SWAGGER_YAML_FILE"
  exit 1
fi

# PATHS_PATHS="$(ls -1 "$PATHS_PATH"/*.yaml | tr '\n' ' ')"
# echo "Merging paths from $INFO_PATH $PATHS_PATHS"
# cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
# PATHS_FILES="$(echo "$PATHS_PATHS" | tr ' ' '\n' | jq -R '[.,inputs]')"
# yq -s --argjson debug "$DEBUG" --argjson file_names "$PATHS_FILES" "$(cat << 'EOF'
#   to_entries | . as $files
#     | reduce $files[] as $file ({};
#       . as $accumulator
#         | if $file.key > 1 and $file.value.paths == null then error("Field .paths not specified for " + $file_names[$file.key]) else . end
#         | (if $debug then [ "Merged paths file", $file_names[$file.key] ] | debug else . end) | $accumulator * $file.value
#       )
# EOF
#   )" "$MERGED_SWAGGER_YAML_FILE.tmp" "$INFO_PATH" $PATHS_PATHS > "$MERGED_SWAGGER_YAML_FILE"
# rm "$MERGED_SWAGGER_YAML_FILE.tmp"

SCHEMAS_PATHS="$(ls -1 "$SCHEMAS_PATH"/*.yaml | tr '\n' ' ')"
CRD_PATHS="$(ls -1 "$CRDS_PATH"/*.yaml | tr '\n' ' ')"
echo "Merging types from $(ls -1 "$SCHEMAS_PATH"/*.yaml | tr '\n' ' ')"
cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
SCHEMAS_FILES="$(echo "$SCHEMAS_PATHS" | tr ' ' '\n' | jq -R '[.,inputs]')"
CRD_FILES="$(echo "$CRD_PATHS" | tr ' ' '\n' | jq -R '[.,inputs]')"
yq -s --argjson debug "$DEBUG" --argjson schema_names "$SCHEMAS_FILES" --argjson crd_names "$CRD_FILES" "$(cat << 'EOF'
  to_entries | . as $files
    | reduce ($files[] | select(.key > 0 and .key < ($schema_names | length))) as $file ($files[0].value;
      . as $accumulator
        | if $file.value.type == null
          then error("Field .type not specified for " + $schema_names[$file.key])
          else . end
        | if $file.value.crdFile == null and $file.value.schema == null
          then error("Field .schema not specified for " + $schema_names[$file.key])
          else . end
        | (
          if $file.value.crdFile != null
          then
            ($crd_names | to_entries[] | select(.value | endswith("/" + $file.value.crdFile)).key + ($schema_names | length)) as $crd_file_index
            | true | ([{
                key: $file.value.type,
                value: (
                    {
                      schema: ([{
                          key: $file.value.type,
                          value: (if $debug then [ "Merged CRD", $file.value.type, $file.value.crdFile, $crd_file_index, $files[$crd_file_index].value.spec.versions[0].schema.openAPIV3Schema ] | debug else . end)
                            | $files[$crd_file_index].value.spec.versions[0].schema.openAPIV3Schema
                        }] | from_entries)
                    } * $file.value
                  ).schema[$file.value.type]
              }] | from_entries)
          else
            ([{
                key: $file.value.type,
                value: $file.value.schema[$file.value.type]
              }] | from_entries)
          end
          ) as $added
            | (if $debug then [ "Source DTO", $file.value.type, $accumulator.components.schemas[$file.value.type] ] | debug else . end)
            | (if $debug then [ "Added DTO", $file.value.type, $added ] | debug else . end)
            | (if $debug then [ "Merged DTO", $file.value.type, $added ] | debug else . end)
            | $accumulator *
              {
                components: {
                  schemas: $added
                }
              }
      )
EOF
  )" "$MERGED_SWAGGER_YAML_FILE.tmp" $SCHEMAS_PATHS $CRD_PATHS > "$MERGED_SWAGGER_YAML_FILE"
rm "$MERGED_SWAGGER_YAML_FILE.tmp"

KNOWN_TYPES=" $(yq -s -r '.[] | .type' "$SCHEMAS_PATH"/*.yaml | tr '\n' ' ') "
ORPHAN_TYPES="$(
for TYPE in $(yq -r '.components.schemas|keys|.[]' "$MERGED_SWAGGER_YAML_FILE")
do
  if ! cat << EOF | grep -qF " $TYPE "
$KNOWN_TYPES
EOF
  then
    printf "$TYPE "
  fi
done)"
DELETE_ORPHANS_FILTER="$(
echo '.'
for TYPE in $ORPHAN_TYPES
do
  echo " | del(.components.schemas[\"$TYPE\"])"
done)"

echo "Removing orphan types $ORPHAN_TYPES"
cp "$MERGED_SWAGGER_YAML_FILE" "$MERGED_SWAGGER_YAML_FILE.tmp"
yq -y "$DELETE_ORPHANS_FILTER" \
  "$MERGED_SWAGGER_YAML_FILE.tmp" > "$MERGED_SWAGGER_YAML_FILE"
rm "$MERGED_SWAGGER_YAML_FILE.tmp"

for TYPE in $ORPHAN_TYPES
do
  sed -i "/^\s\+\$ref: '#\/components\/schemas\/$TYPE'$/d" \
    "$MERGED_SWAGGER_YAML_FILE"
done

REQUIRED_PATHS="$(yq -r '. as $o|paths|select(.[0] == "paths" and .[(length - 1)] == "$ref")|. as $a|$o|getpath($a)|split("/")|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq)"
DEFINED_PATHS="$(yq -r '. as $o|paths|select(.[0] == "components" and .[1] == "schemas" and (.|length) == 3)|.[(length - 1)]' "$MERGED_SWAGGER_YAML_FILE" | sort | uniq)"
if [ "$REQUIRED_PATHS" != "$DEFINED_PATHS" ]
then
  echo "Some types are missing, please add them to the stackgres-k8s/src/api-web/src/main/swagger folder."
  echo
  echo "Required types:"
  echo
  echo "$REQUIRED_PATHS"
  echo
  echo "Defined types:"
  echo
  echo "$DEFINED_PATHS"
  echo
  exit 1
fi

NULL_PATHS="$(yq -c -r "$(cat << 'EOF'
  def allpaths:
    def conditional_recurse(f):  def r: ., (select(.!=null) | f | r); r;
    path(conditional_recurse(.[]?)) | select(length > 0);

  . as $o|allpaths|. as $a|select(($o | getpath($a)) == null)
EOF
    )" "$MERGED_SWAGGER_YAML_FILE")"
if [ -n "$NULL_PATHS" ]
then
  echo "Some fields are null, please review files in the stackgres-k8s/src/api-web/src/main/swagger folder for the following paths:"
  echo
  echo "$NULL_PATHS"
  echo
  exit 1
fi

yq . "$MERGED_SWAGGER_YAML_FILE" > "$MERGED_SWAGGER_JSON_FILE"
