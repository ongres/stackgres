#!/bin/sh
export BUILD_OPERATOR="${BUILD_OPERATOR:-false}"

STACKGRES_VERSION="${STACKGRES_VERSION:-development}"
MATRIX_FORMAT="| %-16s | %10s | %6s |\n"

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

    BUILD_OPERATOR
      If is set to true, it will compile the stackgres and deploy it to the testing cluster, 
      if not it will download stackgres from dockerhub

      
    "
    echo "$HELP"
}

run_tests(){

  LOG_FOLDER="cross-k8s/$IMAGE_TAG-$K8S_VERSION"
  mkdir "$LOG_FOLDER" 
  export REUSE_K8S=false

  export TARGET_PATH="$LOG_FOLDER/target"
  export OPERATOR_PULL_POLICY="Always"

  if [ BUILD_OPERATOR = true ]
  then
    OPERATOR_PULL_POLICY = "Never"
  fi

  sh run-all-tests.sh > "$LOG_FOLDER/$IMAGE_TAG-$K8S_VERSION.log" 2>&1  
 
  if [ $? -eq 0 ]
  then
    print_matrix_line "$IMAGE_TAG" "$K8S_VERSION" "Pass"    
  else
    print_matrix_line "$IMAGE_TAG" "$K8S_VERSION" "Fail"    
  fi
    
}

generate_matrix(){
  print_matrix_line "Stackgres" "Kubernetes" "Result"
  print_matrix_line "----------------" "----------" "------"

  #Cleaing log folder
  rm -rf cross-k8s || true
  mkdir "cross-k8s" 

  while read K8S_VERSION
  do
    export KUBERNETES_VERSION=$K8S_VERSION

    export IMAGE_TAG="$STACKGRES_VERSION-jvm"
    run_tests

    if [ $BUILD_OPERATOR = false ]
    then
      export IMAGE_TAG=$STACKGRES_VERSION
      run_tests
    fi

  done < k8s-versions.txt

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


