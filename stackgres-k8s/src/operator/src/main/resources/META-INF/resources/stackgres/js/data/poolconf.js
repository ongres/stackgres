var poolconf = [{
    "apiVersion": "stackgres.io/v1alpha1",
    "kind": "StackGresConnectionPoolingConfig",
    "metadata": {
        "creationTimestamp": "2019-09-11T11:58:45Z",
        "generation": 1,
        "name": "pgbouncerconf",
        "namespace": "default",
        "resourceVersion": "706",
        "selfLink": "/apis/stackgres.io/v1alpha1/namespaces/default/sgconnectionpoolingconfigs/pgbouncerconf",
        "uid": "6b7dcd30-d70a-4d5e-9b32-e2a7f6ebb62a"
    },
    "spec": {
        "pgbouncer.ini": {
            "default_pool_size": "200",
            "max_client_conn": "200",
            "pool_mode": "transaction"
        },
        "pgbouncerVersion": "1.11"
    }
},
]