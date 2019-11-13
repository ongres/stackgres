#!/bin/bash
echo "Preparing environment"
. envs/init.sh
. envs/kind.sh

echo "Functional tests results" > results.log

source testlib.sh

for s in $(find -name '*-spec.sh')
do
  spec "$s" &
done
wait
cat results.log