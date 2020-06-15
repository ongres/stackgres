#!/bin/sh


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
command -v jq > /dev/null || message_and_exit "The program `jq` is required to be in PATH" 8
command -v yq > /dev/null || message_and_exit "The program `yq` is required to be in PATH" 16

for crd in $1/*.yaml
do
	linter ${crd} || message_and_exit "YAML lint failed for ${crd}" 32
	crd_name=`yq -r '.spec.names.kind' $crd`
	output_filename="crd-${crd_name}-EN.json"
	yq '.' $crd \
		| jq '.spec .validation .openAPIV3Schema .properties' > $2/$output_filename
done
