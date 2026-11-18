---
Title: Maintenance with Zero-Downtime 
weight: 12
url: /runbooks/zero-downtime
description: Achieve Zero-Downtime for Maintenance Tasks
showToc: true
---

Usually maintenance task for Postgres requires some degree of disruption of the service that may convert,
 in some cases, into an issue for your business. 

In this runbook we'll demonstrate a technique to achieve Zero-Downtime for your maintenance task using
 StackGres and an external PgBouncer instance in [transaction pooling mode](https://www.pgbouncer.org/config.html#pool_mode).

>**IMPORTANT:** When transaction pooling is used clients must not use any session-based features, since each transaction ends up in a different connection and thus gets a different session state. From version 1.21.0
PgBouncer added some support to allow use of prepared statement in transaction pooling mode.

## Restart and Switchover with Zero-Downtime

In some cases Postgres have to be restarted in order to allow some parameter changes. This can be performed
 manually using `patronictl restart` command. In other cases you need to re-create the Pod in order for
 some changes in the configuration to take place like after an upgrade of the StackGres operator or when any
 change in the SGCluster modify the Pod. To re-create a Pod you may simply delete it. But in both cases, a
 manual operation, may be dangerous and not taking into account the order of how to re-create each Pod of the
 SGCluster. In general a restart, security upgrade or minor version upgrade SGDbOps are the way to go. They
 will handle smoothly the operation performing a controlled switchover of the primary instance when needed.
 And following the right order to update all of your Pods that needs to be updated.

The switchover operation is not perfect since it disconnect all the Postgres clients forcing them to return
 an error to the final user. PgBouncer offer a mechanism to avoid that when configured with transaction
 pooling mode with the `PAUSE` and `RESUME` commands. Sending the `PAUSE` command with transaction pooling
 will wait for all the connection to complete the current transaction and pause all of them. Sending the
 `RESUME` command will resume the connection in order for them to continue to send transactions to the
 target Postgres instance. While connections are paused we can change the target Postgres instance achieving
 a zero-downtime experience for our users.

To achieve this feature we create a PgBouncer instance (using the same image used by SGCluster for the connection pooling) that will target the read-write Service of an SGCluster. The following snippet allow to create it together with a Service that will be used by applications to connect to PgBouncer:

```yaml
cat << 'EOF' | kubectl replace --force -f -
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pgbouncer
spec:
  selector:
    matchLabels:
      app: pgbouncer
  template:
    metadata:
      labels:
        app: pgbouncer
    spec:
      terminationGracePeriodSeconds: 0
      containers:
      - name: pgbouncer
        image: quay.io/ongres/pgbouncer:v1.22.1-build-6.33
        command:
        - sh
        - /usr/local/bin/start-pgbouncer.sh
        ports:
        - containerPort: 5432
          name: pgbouncer
          protocol: TCP
        volumeMounts:
        - name: dynamic
          mountPath: /etc/pgbouncer
        - name: config
          mountPath: /etc/pgbouncer/pgbouncer.ini
          subPath: pgbouncer.ini
        - name: config
          mountPath: /usr/local/bin/start-pgbouncer.sh
          subPath: start-pgbouncer.sh
      volumes:
      - name: dynamic
        emptyDir: {}
      - name: config
        configMap:
          defaultMode: 0444
          name: pgbouncer
          optional: false
---
apiVersion: v1
kind: Service
metadata:
  name: pgbouncer
spec:
  type: ClusterIP
  selector:
    app: pgbouncer
  ports:
  - name: pgbouncer
    port: 5432
    protocol: TCP
    targetPort: pgbouncer
EOF
```

The PgBouncer instance reference a ConfigMap containing the configuration that targets the SGCluster primary
 Service, it also contains an initialization scripts that create the credentials for pgbouncer users and to
 query the Postgres instance in order to authenticate other users:

```yaml
cat << 'EOF' | kubectl replace --force -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: pgbouncer
data:
  pgbouncer.ini: |
    [databases]

    * = host=cluster port=5432

    [pgbouncer]
    listen_addr=0.0.0.0
    listen_port=5432

    pool_mode=transaction
    max_client_conn=1000
    default_pool_size=100
    max_db_connections=0
    max_user_connections=0

    auth_type=md5
    auth_file=/etc/pgbouncer/userlist.txt
    auth_user=postgres
    auth_query=SELECT usename, passwd FROM pg_shadow WHERE usename=$1

    admin_users=pgbouncer_admin
    stats_users=pgbouncer_stats
    application_name_add_host=1
    ignore_startup_parameters=extra_float_digits

    server_check_query=;
  start-pgbouncer.sh: |
    #!/bin/sh
    printf '"%s" "%s"\n' "postgres" "sup3rus3r" >> /etc/pgbouncer/userlist.txt
    printf '"%s" "%s"\n' "pgbouncer_admin" "pgb0unc3r" >> /etc/pgbouncer/userlist.txt
    printf '"%s" "%s"\n' "pgbouncer_stats" "pgb0unc3r" >> /etc/pgbouncer/userlist.txt
    exec pgbouncer /etc/pgbouncer/pgbouncer.ini
EOF
```

We then create an SGCluster that has the logic to send the `PAUSE` and `RESUME` command thanks to the
 `before_stop` guard script and `on_role_change` callback:
 
```yaml
cat << 'EOF' | kubectl replace --force -f -
---
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: cluster
spec:
  configurations:
    patroni:
      initialConfig:
        postgresql:
          callbacks:
            on_role_change: /callbacks/on_role_change
          before_stop: /callbacks/before_stop
    credentials:
      users:
        superuser:
          password:
            name: credentials
            key: superuser-password
  instances: 2
  profile: development
  pods:
    customVolumeMounts:
      patroni:
      - name: custom-callbacks
        mountPath: /callbacks
    customVolumes:
    - name: callbacks
      configMap:
        defaultMode: 0775
        name: callbacks
        optional: false
    persistentVolume:
      size: 5Gi
  postgres:
    version: latest
---
apiVersion: v1
kind: Secret
metadata:
  name: credentials
stringData:
  superuser-password: sup3rus3r
EOF
```

The scripts are mounted in a custom volume mount from a ConfigMap. The `before_stop` script is executed
 by Patroni synchronously and blocks the primary instance from being stopped by a switchover or a restart
 until the PAUSE command is sent to the PgBouncer instance. This allows the connection to complete the
 ongoing transactions before the primary goes offline. The `on_role_change` script is executed
 asynchronically by Patroni and do not block the promotion of a primary. It actually waits for the instance
 to be converted to primary and then sends the RESUME command so that connection sent to the instance
 will be able to write to the primary:


```yaml
cat << 'EOF' | kubectl replace --force -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: callbacks
data:
  before_stop: |
    #!/bin/sh
    set -x
    PATRONI_NAME="$(cat /etc/hostname)"
    PATRONI_HISTORY="$(patronictl history -f tsv | tail -n +2)"
    PATRONI_LIST="$(patronictl list -f tsv | tail -n +2)"
    if {
        [ "x$PATRONI_HISTORY" = x ] \
        && ! printf %s "$PATRONI_LIST" | grep -v $'^[^\t]\+\t'"$PATRONI_NAME"$'\t' | grep -q $'^[^\t]\+\t[^\t]\+\t[^\t]\+\tLeader\t'
      } \
      || printf %s "$PATRONI_HISTORY" | grep -q $'^[^\t]\+\t[^\t]\+\t[^\t]\+\t[^\t]\+\t'"$PATRONI_NAME"'$'
    then
      psql postgresql://pgbouncer_admin:pgb0unc3r@pgbouncer/pgbouncer -c PAUSE
    fi
    exit 0
  on_role_change: |
    #!/bin/sh
    set -x
    if [ "$#" = 0 ] || [ "x$2" = xmaster ]
    then
      until psql -tA -c 'SELECT pg_is_in_recovery()' | grep -qxF f
      do
        true
      done
      psql postgresql://pgbouncer_admin:pgb0unc3r@pgbouncer/pgbouncer -c RESUME
      psql postgresql://pgbouncer_admin:pgb0unc3r@pgbouncer/pgbouncer -tA -c 'SHOW STATE' | grep -q 'paused|no'
    fi
EOF
```

Now, to demonstrate the effectivity of this deployment let's create the following Job that will launch a
 pgbench against the PgBouncer instance:

```yaml
cat << 'EOF' | kubectl replace --force -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: pgbench
spec:
  template:
    spec:
      restartPolicy: OnFailure
      terminationGracePeriodSeconds: 0
      containers:
      - name: pgbench
        image: quay.io/ongres/postgres-util:v16.3-build-6.34
        command:
        - sh
        - -c
        - |
          pgbench postgresql://postgres:sup3rus3r@pgbouncer/postgres -i
          pgbench postgresql://postgres:sup3rus3r@pgbouncer/postgres -T 300 -c 4 -j 4 -P 2 --progress-timestamp
EOF
```

Wait for the Job to be started and print some progress of the benchmark, then create an restart SGDbOps that
 will restart the replica, perform a switchover and then will restart the primary.

```yaml
cat << 'EOF' | kubectl replace --force -f -
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: restart
spec:
  op: restart
  sgCluster: cluster
  restart:
    method: InPlace
EOF
```

After the operation is completed wait for the completion of the Job and check no errors were raised:

```
kubectl wait sgdbops restart --for=condition=Completed
kubectl wait job pgbench --for=condition=Completed
```

Check the pgbench Job's logs and you should not be able to find any failed connection!
