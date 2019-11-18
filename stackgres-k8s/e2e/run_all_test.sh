#!/bin/bash
echo "Preparing environment"
. envs/init.sh
. envs/kind.sh &> env.log

prepare_environment "kind"


echo "Functional tests results" > results.log

source testlib.sh

SPECS=$(ls | grep _spec.sh)
for s in $SPECS
do
  spec $s &
done
wait
cat results.log