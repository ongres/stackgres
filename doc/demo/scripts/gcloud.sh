#!/bin/bash
## Export variables
export project = <Your project's name>
export namecluster= <Your cluster's name >
export zone=<zone deployment>
export nodelocations=<location deployment>
export machinetype=<machine type>
export disksize=<disk Size>
export numnodes=<numbers nodes>
export clusterversion=<version kubernetes cluster>

## Create cluster
gcloud  container --project $project  clusters create $namecluster --zone $zone --node-locations $nodelocations  --machine-type $machinetype --disk-size $disksize --num-nodes $numnodes --cluster-version $clusterversion --enable-stackdriver-kubernetes --no-enable-ip-alias --no-enable-autoupgrade --metadata disable-legacy-endpoints=true verbosity=none
