#!/bin/sh

. "$(dirname "$0")/e2e"

E2E_BUILD_OPERATOR="${E2E_BUILD_OPERATOR:-false}"
E2E_INCLUDE_NATIVE="${E2E_INCLUDE_NATIVE:-true}"
GRAALVM_HOME="${GRAALVM_HOME:-/usr/lib/jvm/graalvm}"
MATRIX_FORMAT="| %-16s | %10s | %8s |\n"
STACKGRES_VERSION=${STACKGRES_VERSION:-development}
SPEC="all"
if [ ! -z $1 ]
then
  SPEC="$1"
fi
print_matrix_line(){

  if [ -z $OUTPUT_FILE ]
  then
    printf "$MATRIX_FORMAT" "$@"  
  else
    printf "$MATRIX_FORMAT" "$@" >> $OUTPUT_FILE
  fi
  
}

print_help(){
    HELP="$(basename "$0") [-h] [-o] -- tests stackgres with different kubernetes versions
    It requires the k8s-versions.txt to now which versions to tests.     
where:
    -h | help 
       show this help text
    -o | outputfile 
       (Optional) file to print the matrix. If not set it will be printed in the standart output
    
Environment variables
    STACKGRES_VERSION
      Indicates which stackgres version to use. Default: development
 
    "
    echo "$HELP"
}

execute_test(){
  if [ "$SPEC" = "all" ]
  then
    sh run-all-tests.sh
  else
    sh run-test.sh "$SPEC"
  fi
}
run_tests(){

  export K8S_REUSE=true
  export E2E_BUILD_OPERATOR=false

  get_stackgres_images | while read sv
  do
    export IMAGE_TAG="$sv"
    if [ -z $TARGET_PATH ]
    then
      export TARGET_PATH="$(dirname "$0")/target/$IMAGE_TAG-$K8S_VERSION"
    else
      export TARGET_PATH="$TARGET_PATH/$IMAGE_TAG-$K8S_VERSION"
    fi
    mkdir -p "$TARGET_PATH"
    export K8S_REUSE=true
    export E2E_BUILD_OPERATOR=false

    if ! sh e2e reset_k8s >> "$TARGET_PATH/$IMAGE_TAG-$K8S_VERSION.log" 2>&1
    then
      print_matrix_line "$IMAGE_TAG" "$K8S_VERSION" "Skipped"
      return 0
    fi
    
    if execute_test >> "$TARGET_PATH/$IMAGE_TAG-$K8S_VERSION.log" 2>&1
    then
      print_matrix_line "$IMAGE_TAG" "$K8S_VERSION" "Pass"    
    else
      print_matrix_line "$IMAGE_TAG" "$K8S_VERSION" "Fail"    
    fi
  done
    
}

get_stackgres_images(){
  IMAGES=""
  if [ $E2E_INCLUDE_NATIVE = true ]
  then
    IMAGES=$(get_stackgres_images_with_native)
  else
    IMAGES=$(get_stackgres_images_without_native)
  fi
  echo "$IMAGES"
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
    cd "$STACKGRES_PATH/src"
    ./mvnw -q -DskipTests clean package -P build-image-jvm > "$LOG_PATH/build-image-jvm.log"
  )
  if [ $E2E_INCLUDE_NATIVE = true ]
  then
  (
    cd "$STACKGRES_PATH/src"
    mvn -DskipTests clean package -P native > "$LOG_PATH/build-native.log"
    cp "$GRAALVM_HOME/jre/lib/amd64/libsunec.so" operator/target/
    cp "$GRAALVM_HOME/jre/lib/security/cacerts" operator/target/

    mvn -DskipTests package -P build-image-native > "$LOG_PATH/build-image-native.log"
  )
  fi
}

generate_matrix(){
  print_matrix_line "Stackgres" "Kubernetes" "Result"
  print_matrix_line "----------------" "----------" "--------"

  if [ $E2E_BUILD_OPERATOR = true ]
  then
    build_images
  fi 
  
  get_k8s_versions | while read v
  do
    export K8S_VERSION="$v"

    run_tests "$v"
    
  done
}

while getopts o:h-: OPT; do
  if [ "$OPT" = "-" ]; then   
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


