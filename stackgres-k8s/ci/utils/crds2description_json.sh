#!/bin/sh

SCRIPTPATH=$(dirname "$(readlink -f "$0")")

message_and_exit() {
	echo "\n\t$1\n\n"
	exit $2
}

usage() {
	message_and_exit "Usage: $0 <path_to_crds_dir> <path_output_dir>" 1
}

# linter() {
#     yamllint -c ${SCRIPTPATH}/yamllint-config.yaml ${1}
# }

[ $# -eq 2 ] || usage

[ -d $1 ] || message_and_exit "Dir $1 does not exist" 2
[ -d $2 ] || message_and_exit "Dir $2 does not exist" 4
command -v jq > /dev/null || message_and_exit "The program `jq` is required to be in PATH" 8
command -v yq > /dev/null || message_and_exit "The program `yq` is required to be in PATH" 16

jq_script="/tmp/.tempfile-$RANDOM-$RANDOM"

cat << 'EOF' > $jq_script
def extract_properties:
    to_entries
    | map({
        key: .key,
        value: (
            if .value.type == "object" and .value.properties != null
            then .value.properties | extract_properties
            else .value.description
            end
        )
    })
    | from_entries
    ;

def flatten_to_dot_notation:
    [
        leaf_paths as $path
        | { "key": $path | join("."), "value": getpath($path) }
    ]
    | from_entries
    ;

.spec .validation .openAPIV3Schema .properties | extract_properties | flatten_to_dot_notation
EOF

for crd in $1/*.yaml
do
    #linter ${crd}
	crd_name=`yq -r '.spec.names.kind' $crd`
	output_filename="crd-${crd_name}-description-EN.json"
	yq '.' $crd | jq -f $jq_script > $2/$output_filename
done

rm -f $jq_script
