/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;
import com.ongres.junit.docker.DockerContainer;
import com.ongres.junit.docker.DockerExtension;
import com.ongres.junit.docker.WhenReuse;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operatorframework.resource.EventReason;
import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@DockerExtension({
    @DockerContainer(
        alias = "k8s",
        extendedBy = K8sConfiguration.class,
        whenReuse = WhenReuse.ALWAYS,
        stopIfChanged = true)
})
@EnabledIfEnvironmentVariable(named = "ENABLE_IT", matches = "true")
public class StackGresOperatorIt extends AbstractStackGresOperatorIt {

  private static final String CLUSTER_NAME = "stackgres";

  @Test
  public void createClusterTest(@ContainerParam("k8s") Container k8s) throws Exception {
    ItHelper.installStackGresConfigs(k8s, namespace);
    ItHelper.installStackGresCluster(k8s, namespace, CLUSTER_NAME, 1);
    checkStackGresEvent(k8s, ClusterEventReason.CLUSTER_CREATED, StackGresCluster.class);
    checkStackGresCluster(k8s, 1);
    ItHelper.upgradeStackGresCluster(k8s, namespace, CLUSTER_NAME, 2);
    checkStackGresEvent(k8s, ClusterEventReason.CLUSTER_UPDATED, StackGresCluster.class);
    checkStackGresCluster(k8s, 2);
    checkStackGresBackups(k8s);
    ItHelper.deleteStackGresCluster(k8s, namespace, CLUSTER_NAME);
    checkStackGresEvent(k8s, ClusterEventReason.CLUSTER_DELETED, Service.class);
    checkStackGresClusterDeletion(k8s);
  }

  private void checkStackGresEvent(Container k8s, EventReason eventReason,
      Class<? extends HasMetadata> resourceClass) throws Exception {
    ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl get events -n " + namespace + " -o wide"
            + " | sed 's/\\s\\+/ /g' | grep "
            + "'" + eventReason.reason() + " " + resourceClass.getSimpleName() + "'"
            + " && echo 1")),
        s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking creation of event for "
                + " cluster '" + CLUSTER_NAME + " in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
  }

  private void checkStackGresCluster(Container k8s, int instances) throws Exception {
    for (int instanceIndex = 0; instanceIndex < instances; instanceIndex++) {
      String instance = String.valueOf(instanceIndex);
      ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
          "kubectl get pod -n  " + namespace + " " + CLUSTER_NAME + "-" + instance
              + " && echo 1")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking creation of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
          "kubectl describe pod -n  " + namespace + " " + CLUSTER_NAME + "-" + instance)),
          s -> s.anyMatch(line -> line.matches("  Ready\\s+True\\s*")), 180, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking availability of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
          "kubectl exec -t -n " + namespace + " "
              + CLUSTER_NAME + "-" + instance + " -c postgres-util --"
              + " sh -c \"psql -t -A -U postgres -d postgres -p " + EnvoyUtil.PG_PORT
              + " -c 'SELECT 1'\"")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking connection available to postgres of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
          "kubectl exec -t -n " + namespace + " "
              + CLUSTER_NAME + "-" + instance + " -c postgres-util --"
              + " sh -c \"PGPASSWORD=$(kubectl get secret " + CLUSTER_NAME + " -n " + namespace
              + " -o yaml | grep superuser-password | cut -d ':' -f 2 | tr -d ' ' | base64 -d)"
              + " psql -t -A -U postgres -d postgres -p " + EnvoyUtil.PG_ENTRY_PORT
              + " -h localhost"
              + " -c 'SELECT 1'\"")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking connection available to postgres of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
          "kubectl exec -t -n " + namespace + " "
              + CLUSTER_NAME + "-" + instance + " -c postgres-util --"
              + " sh -c \"PGPASSWORD=$(kubectl get secret " + CLUSTER_NAME + " -n " + namespace
              + " -o yaml | grep superuser-password | cut -d ':' -f 2 | tr -d ' ' | base64 -d)"
              + " psql -t -A -U postgres -d postgres -p 5433 -h localhost  -c 'SELECT 1'\"")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking connection available to postgres of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
    }
  }

  private void checkStackGresBackups(Container k8s)
      throws InterruptedException, Exception {
    String currentWalFileName = k8s.execute("sh", "-l", "-c",
        "kubectl exec -t -n " + namespace + " " + CLUSTER_NAME + "-" + 0
            + " -c postgres-util -- sh -c \"psql -t -A -U postgres -p " + EnvoyUtil.PG_PORT
            + " -c 'SELECT r.file_name from pg_walfile_name_offset(pg_current_wal_lsn()) as r'\"")
        .filter(ItHelper.EXCLUDE_TTY_WARNING)
        .findFirst()
        .get();
    ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl exec -t -n " + namespace + " " + CLUSTER_NAME + "-" + 0
            + " -c postgres-util -- sh -c \"psql -t -A -U postgres -p " + EnvoyUtil.PG_PORT
            + " -c 'SELECT r.file_name from pg_walfile_name_offset(pg_switch_wal()) as r'\"")),
        s -> s.anyMatch(newWalFileName -> newWalFileName.equals(currentWalFileName)), 60,
        ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while waiting switch of wal file " + currentWalFileName + ":\n"
                + s.collect(Collectors.joining("\n"))));
    ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl exec -t -n " + namespace + " "
            + CLUSTER_NAME + "-" + 0 + " -c patroni --"
            + " sh -c \"exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value() + "'"
            + " wal-g wal-fetch " + currentWalFileName
            + " /tmp/" + currentWalFileName + " && echo 1\"")),
        s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking archive_command is working properly:\n"
                + s.collect(Collectors.joining("\n"))));
    ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl exec -t -n " + namespace + " "
            + CLUSTER_NAME + "-" + 0 + " -c patroni --"
            + " sh -c \"exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value() + "'"
            + " wal-g backup-list | grep -n . | cut -d : -f 1\"")),
        s -> s.anyMatch(line -> line.equals("2")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking full backup are working properly:\n"
                + s.collect(Collectors.joining("\n"))));
  }

  private void checkStackGresClusterDeletion(Container k8s) throws Exception {
    ItHelper.waitUntil(Unchecked.supplier(() -> k8s.execute("sh", "-l", "-c",
        "kubectl describe statefulset -n  " + namespace + " " + CLUSTER_NAME + " || echo 1")),
        s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking deletion of"
                + " StatefulSet '" + CLUSTER_NAME
                + "' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
  }
}
