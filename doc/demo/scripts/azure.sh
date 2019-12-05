#!/bin/bash
## Export variables
export namecluster=<Your cluster's name>
export location=<Your location  of your project>
export nodes=<number of nodes>
export namegroup=<you name group>

## Create Group
az group create --name $namegroup --location $location

## Create cluster

az aks create --name $namecluster --resource-group $namegroup --location $location --node-count $nodes
