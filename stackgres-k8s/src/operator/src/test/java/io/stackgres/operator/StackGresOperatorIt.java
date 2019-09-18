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

import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@DockerExtension({
  @DockerContainer(
      alias = "kind",
      extendedBy = KindConfiguration.class,
      whenReuse = WhenReuse.ALWAYS,
      stopIfChanged = true)
})
public class StackGresOperatorIt extends AbstractStackGresOperatorIt {

  private final String CLUSTER_NAME = "test";

  @Test
  public void createClusterTest(@ContainerParam("kind") Container kind) throws Exception {
    ItHelper.installStackGresConfigs(kind, namespace);
    ItHelper.installStackGresCluster(kind, namespace, CLUSTER_NAME, 1);
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
        "kubectl get events -n " + namespace + " -o wide"
            + " | sed 's/\\s\\+/ /g' | grep 'ClusterCreated StackGresCluster' && echo 1 || true")),
        s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking creation of event for "
                + " cluster '" + CLUSTER_NAME + " in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
    checkStackGresCluster(kind, 1);
    ItHelper.upgradeStackGresCluster(kind, namespace, CLUSTER_NAME, 2);
    checkStackGresCluster(kind, 2);
    ItHelper.deleteStackGresCluster(kind, namespace, CLUSTER_NAME);
    checkStackGresClusterDeletion(kind);
  }

  private void checkStackGresCluster(Container kind, int instances) throws Exception {
    for (int instanceIndex = 0; instanceIndex < instances; instanceIndex++) {
      String instance = String.valueOf(instanceIndex);
      ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
          "kubectl get pod -n  " + namespace + " " + CLUSTER_NAME + "-" + instance
              + " && echo 1 || true")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking creation of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
          "kubectl describe pod -n  " + namespace + " " + CLUSTER_NAME + "-" + instance)),
          s -> s.anyMatch(line -> line.matches("  Ready\\s+True\\s*")), 180, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking availability of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
          "kubectl exec -t -n " + namespace + " "
              + CLUSTER_NAME + "-" + instance + " -c postgres-util --"
              + " sh -c \"psql -t -A -U postgres -p 5432 -c 'SELECT 1'\"")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking connection available to postgres of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
      ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
          "kubectl exec -t -n " + namespace
              + " " + CLUSTER_NAME + "-" + instance + " -c postgres-util --"
              + " sh -c \"PGPASSWORD=$(kubectl get secret " + CLUSTER_NAME + " -n " + namespace
              + " -o yaml | grep superuser-password | cut -d ':' -f 2 | tr -d ' ' | base64 -d)"
              + " psql -t -A -U postgres -d postgres -p 6432 -c 'SELECT 1'\"")),
          s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
          s -> Assertions.fail(
              "Timeout while checking connection available to pgbouncer of"
                  + " pod '" + CLUSTER_NAME + "-" + instance
                  + "' in namespace '" + namespace + "':\n"
                  + s.collect(Collectors.joining("\n"))));
    }
  }

  private void checkStackGresClusterDeletion(Container kind) throws Exception {
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("sh", "-l", "-c",
        "kubectl describe statefulset -n  " + namespace + " " + CLUSTER_NAME + " || echo 1")),
        s -> s.anyMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking deletion of"
                + " StatefulSet '" + CLUSTER_NAME
                + "' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
  }
}
