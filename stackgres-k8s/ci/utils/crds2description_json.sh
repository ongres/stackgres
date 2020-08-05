#!/bin/sh

set -e

SCRIPT_PATH=$(dirname "$(readlink -f "$0")")

message_and_exit() {
	echo "\n\t$1\n\n"
	exit $2
}

usage() {
	message_and_exit "Usage: $0 <path_to_crds_dir> <path_output_dir>" 1
}

linter() {
    yamllint -c ${SCRIPT_PATH}/yamllint-config.yaml ${1}
}

[ $# -eq 2 ] || usage

[ -d $1 ] || message_and_exit "Dir $1 does not exist" 2
[ -d $2 ] || message_and_exit "Dir $2 does not exist" 4
command -v jq > /dev/null || message_and_exit 'The program `jq` is required to be in PATH' 8
command -v yq > /dev/null || message_and_exit 'The program `yq` (https://kislyuk.github.io/yq/) is required to be in PATH' 16
command -v yamllint > /dev/null || message_and_exit 'The program `yamllint` is required to be in PATH' 32

for crd in $1/*.yaml
do
	linter ${crd} || message_and_exit "YAML lint failed for ${crd}" 33
	crd_name=`yq -r '.spec.names.kind' $crd`
	output_filename="crd-${crd_name}-EN.json"
	yq '.' $crd \
		| jq '.spec .validation .openAPIV3Schema .properties' > $2/$output_filename
done
