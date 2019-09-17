//var clusters = [];

/*var pgcluster = [ 
{
  "apiVersion": "stackgres.io/v1alpha1",
  "kind": "StackGresCluster",
  "metadata": {
    "additionalProperties": {},
    "creationTimestamp": "2019-09-12T09:19:24Z",
    "finalizers": [],
    "generation": 1,
    "name": "stackgres",
    "namespace": "default",
    "ownerReferences": [],
    "resourceVersion": "1041",
    "selfLink": "/apis/stackgres.io/v1alpha1/namespaces/default/sgclusters/stackgres",
    "uid": "698ab4ad-d53e-11e9-a6aa-0242ac110003"
  },
  "spec": {
    "connectionPoolingConfig": "pgbouncerconf",
    "instances": 3,
    "postgresConfig": "postgresconf",
    "postgresVersion": "11.5",
    "resourceProfile": "size-xs",
    "sidecars": [
      "connection-pooling",
      "postgres-utils"
    ],
    "storageClass": "standard",
    "volumeSize": "5Gi"
  },
  "status": {
    "averageLoad10m": "2.57",
    "averageLoad1m": "2.64",
    "averageLoad5m": "2.68",
    "cpuFound": "4",
    "cpuRequested": "500m",
    "diskFound": "7.7Gi",
    "diskUsed": "615Mi",
    "memoryFound": "16Gi",
    "memoryRequested": "512Mi",
    "memoryUsed": "14Gi",
    "pods": [
      {
        "containers": "3",
        "containersReady": "3",
        "name": "stackgres-0",
        "namespace": "default",
        "role": "replica",
        "status": "Running"
      },
      {
        "containers": "3",
        "containersReady": "3",
        "name": "stackgres-1",
        "namespace": "default",
        "role": "master",
        "status": "Running"
      },
      {
        "containers": "3",
        "containersReady": "0",
        "name": "stackgres-2",
        "namespace": "default",
        "role": "replica",
        "status": "Pending"
      }
    ],
    "podsReady": "2"
  }
},
{
  "apiVersion": "stackgres.io/v1alpha1",
  "kind": "StackGresCluster",
  "metadata": {
    "additionalProperties": {},
    "creationTimestamp": "2019-09-12T09:19:24Z",
    "finalizers": [],
    "generation": 1,
    "name": "ongres",
    "namespace": "default",
    "ownerReferences": [],
    "resourceVersion": "1041",
    "selfLink": "/apis/stackgres.io/v1alpha1/namespaces/default/sgclusters/stackgres",
    "uid": "698ab4ad-d53e-11e9-a6aa-0242ac110003"
  },
  "spec": {
    "connectionPoolingConfig": "pgbouncerconf",
    "instances": 3,
    "postgresConfig": "postgresconf",
    "postgresVersion": "11.5",
    "resourceProfile": "size-xs",
    "sidecars": [
      "connection-pooling",
      "postgres-utils"
    ],
    "storageClass": "standard",
    "volumeSize": "5Gi"
  },
  "status": {
    "averageLoad10m": "2.57",
    "averageLoad1m": "2.64",
    "averageLoad5m": "2.68",
    "cpuFound": "4",
    "cpuRequested": "500m",
    "diskFound": "7.7Gi",
    "diskUsed": "615Mi",
    "memoryFound": "16Gi",
    "memoryRequested": "512Mi",
    "memoryUsed": "14Gi",
    "pods": [
      {
        "containers": "3",
        "containersReady": "3",
        "name": "ongres-A",
        "namespace": "default",
        "role": "replica",
        "status": "Running"
      },
      {
        "containers": "3",
        "containersReady": "3",
        "name": "ongres-B",
        "namespace": "default",
        "role": "master",
        "status": "Running"
      },
      {
        "containers": "3",
        "containersReady": "0",
        "name": "ongres-C",
        "namespace": "default",
        "role": "replica",
        "status": "Pending"
      }
    ],
    "podsReady": "2"
  }
},
{
  "apiVersion": "stackgres.io/v1alpha1",
  "kind": "StackGresCluster",
  "metadata": {
    "additionalProperties": {},
    "creationTimestamp": "2019-09-12T09:19:24Z",
    "finalizers": [],
    "generation": 1,
    "name": "gitlab",
    "namespace": "default",
    "ownerReferences": [],
    "resourceVersion": "1041",
    "selfLink": "/apis/stackgres.io/v1alpha1/namespaces/default/sgclusters/stackgres",
    "uid": "698ab4ad-d53e-11e9-a6aa-0242ac110003"
  },
  "spec": {
    "connectionPoolingConfig": "pgbouncerconf",
    "instances": 3,
    "postgresConfig": "postgresconf",
    "postgresVersion": "11.5",
    "resourceProfile": "size-xs",
    "sidecars": [
      "connection-pooling",
      "postgres-utils"
    ],
    "storageClass": "standard",
    "volumeSize": "5Gi"
  },
  "status": {
    "averageLoad10m": "2.57",
    "averageLoad1m": "2.64",
    "averageLoad5m": "2.68",
    "cpuFound": "4",
    "cpuRequested": "500m",
    "diskFound": "7.7Gi",
    "diskUsed": "615Mi",
    "memoryFound": "16Gi",
    "memoryRequested": "512Mi",
    "memoryUsed": "14Gi",
    "pods": [
      {
        "containers": "3",
        "containersReady": "3",
        "name": "gitlab-X",
        "namespace": "default",
        "role": "master",
        "status": "running"
      },
      {
        "containers": "3",
        "containersReady": "3",
        "name": "gitlab-Y",
        "namespace": "default",
        "role": "master",
        "status": "Running"
      },
      {
        "containers": "3",
        "containersReady": "0",
        "name": "gitlab-Z",
        "namespace": "default",
        "role": "replica",
        "status": "Pending"
      }
    ],
    "podsReady": "2"
  }
},
]*/

/*pgcluster.forEach(function(item, index){
  var cluster = { 
    name: item.metadata.name,
    data: item 
  };
  clusters[item.metadata.name] = cluster;
});*/