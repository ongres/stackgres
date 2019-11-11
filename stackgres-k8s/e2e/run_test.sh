#!/bin/bash
echo "Preparing environment"
. envs/init.sh
. envs/kind.sh &> env.log

echo "Functional tests results" > results.log

source testlib.sh

if [ -z "$1" ]
then
  >&2 echo "Must specify a test to run"
  exit 1
fi

spec "$1"
cat results.log
