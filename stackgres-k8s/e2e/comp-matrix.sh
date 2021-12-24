#!/bin/sh

. "${0%/*}/e2e"

E2E_BUILD_IMAGES="${E2E_BUILD_IMAGES:-false}"
E2E_INCLUDE_NATIVE="${E2E_INCLUDE_NATIVE:-true}"
GRAALVM_HOME="${GRAALVM_HOME:-/usr/lib/jvm/graalvm}"
MATRIX_FORMAT="| %-11s | %-16s | %16s | %-28s | %8s |\n"
STACKGRES_VERSION="${STACKGRES_VERSION:-main}"
SPEC="all"
ROOT_TARGET_PATH="${TARGET_PATH}"

if [ ! -z "$1" ]
then
  SPEC="$1"
fi

print_matrix_line(){
  if [ -z "$OUTPUT_FILE" ]
  then
    printf "$MATRIX_FORMAT" "$@"  
  else
    printf "$MATRIX_FORMAT" "$@" >> "$OUTPUT_FILE"
  fi
}

print_help(){
    HELP="$(basename "$0") [-h] [-o] -- tests stackgres with different kubernetes versions
    It requires the get_k8s_versions to be implemented in the environment to tests.     
where:
    -h | help 
       show this help text
    -o | outputfile 
       (Optional) file to print the matrix. If not set it will be printed in the standard output
    
Environment variables
    STACKGRES_VERSION
      Indicates which stackgres version to use. Default: main
 
    "
    echo "$HELP"
}

execute_test(){
  export TARGET_PATH="$ROOT_TARGET_PATH/$IMAGE_TAG-$K8S_VERSION"
  mkdir -p "$TARGET_PATH"
  
  k8s_webhook_cleanup
  helm_cleanup
  k8s_cleanup

  if [ "$SPEC" = "all" ]
  then
    sh run-all-tests.sh
  else
    sh run-test.sh "$SPEC"
  fi
  
}

get_spec_name(){
  echo "$SPEC" | awk -F '/' '{print $NF}'
}

run_tests(){
  local RESULT
  local EXIT_CODE
  try_function reset_k8s >> "$TARGET_PATH/$K8S_VERSION.log" 2>&1
  if "$RESULT"
  then
    export K8S_REUSE=true
    export E2E_BUILD_IMAGES=false

    for sv in $(get_stackgres_images)
    do
      export IMAGE_TAG="$sv"
      export TARGET_PATH="$ROOT_TARGET_PATH/$IMAGE_TAG-$K8S_VERSION"
      mkdir -p "$TARGET_PATH"
      try_function execute_test >> "$TARGET_PATH/$IMAGE_TAG-$K8S_VERSION.log" 2>&1
      if "$RESULT"
      then
        print_matrix_line "$E2E_ENV" "$IMAGE_TAG" "$K8S_VERSION" "$(get_spec_name)" "Pass"
      else
        print_matrix_line "$E2E_ENV" "$IMAGE_TAG" "$K8S_VERSION" "$(get_spec_name)" "Fail"
      fi
      
    done
  else
    for sv in $(get_stackgres_images)
    do
      export IMAGE_TAG="$sv"
      print_matrix_line "$E2E_ENV" "$IMAGE_TAG" "$K8S_VERSION" "$(get_spec_name)" "Skipped"
    done
  fi

  delete_k8s >> "$TARGET_PATH/$IMAGE_TAG-$K8S_VERSION.log" 2>&1 || true
}

get_stackgres_images(){
  if [ "$E2E_INCLUDE_NATIVE" = true ]
  then
    get_stackgres_images_with_native
  else
    get_stackgres_images_without_native
  fi
}

get_stackgres_images_with_native(){
  cat << EOF 
$STACKGRES_VERSION-jvm
$STACKGRES_VERSION
EOF
}

get_stackgres_images_without_native(){
  cat << EOF 
$STACKGRES_VERSION-jvm
EOF
}

build_images(){
  (
    cd "$STACKGRES_PATH/stackgres-k8s/src"
    ./mvnw -q clean package -DskipTests -Dmaven.test.skip=true -P build-image-jvm > "$LOG_PATH/build-image-jvm.log"
  )
  if [ "$E2E_INCLUDE_NATIVE" = true ]
  then
  (
    cd "$STACKGRES_PATH/stackgres-k8s/src"
     ./mvnw -q clean package -DskipTests -Dmaven.test.skip=true -P native > "$LOG_PATH/build-native.log"
    cp "$GRAALVM_HOME/jre/lib/amd64/libsunec.so" operator/target/
    cp "$GRAALVM_HOME/jre/lib/security/cacerts" operator/target/

     ./mvnw -q clean package -DskipTests -Dmaven.test.skip=true package -P build-image-native > "$LOG_PATH/build-image-native.log"
  )
  fi
}

generate_matrix(){
  print_matrix_line "Environment" "Stackgres" "Kubernetes" "Test" "Result"
  print_matrix_line "-----------" "----------------" "----------------" "----------------------------" "--------"

  if [ "$E2E_BUILD_IMAGES" = true ]
  then
    build_images
  fi 
  
  for v in $(get_k8s_versions)
  do
    export K8S_VERSION="$v"

    run_tests "$v"
  done
}

while getopts o:h-: OPT; do
  if [ "$OPT" = "-" ]
  then
    OPT="${OPTARG%%=*}"
    OPTARG="${OPTARG#$OPT}"
    OPTARG="${OPTARG#=}"
  fi
  case "$OPT" in
    o | outputfile ) OUTPUT_FILE="$OPTARG" ;;
    h | help ) print_help
       exit 0
       ;;
    \?) printf "Illegal option: -%s\n" "$OPTARG" >&2
       print_help >&2
       exit 1
       ;;
  esac
done

generate_matrix
