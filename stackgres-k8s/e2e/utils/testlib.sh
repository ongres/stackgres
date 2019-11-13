function run_test(){
    TEST_NAME=$1
    LOG_FILE=${TEST_NAME//" "/"-"}.log
    echo "Running test $TEST_NAME"
    ($2) > $LOG_FILE 2>&1
    if [ $? -eq 0 ]
    then
      cat $LOG_FILE
      echo "$TEST_NAME. SUCCESS."
      exit 0
    else
      cat $LOG_FILE
      echo "$TEST_NAME. FAIL. See file $LOG_FILE.log for details"
      exit 1
    fi
}

function spec(){
    SPEC_FILE=$1
    SPEC_NAME=$(basename "$SPEC_FILE" .sh)
    echo "Running $SPEC_NAME tests"
    if bash $SPEC_FILE > $SPEC_NAME.log 2>&1
    then
      echo "$SPEC_NAME. SUCCESS." >> results.log
    else
      echo "$SPEC_NAME. FAIL. See file $SPEC_NAME.log for details" >> results.log
    fi
}