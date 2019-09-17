package io.stackgres.operator;

import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import com.ongres.junit.docker.Container;
import com.ongres.junit.docker.ContainerParam;
import com.ongres.junit.docker.DockerContainer;
import com.ongres.junit.docker.DockerExtension;
import com.ongres.junit.docker.Environment;
import com.ongres.junit.docker.Mount;
import com.ongres.junit.docker.WaitFor;

import org.jooq.lambda.Unchecked;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@DockerExtension({
  @DockerContainer(
      alias = "kind",
      image = "stackgres/it:latest",
      arguments = { "/bin/bash", "-c",
          "bash /scripts/restart-kind.sh 3;"
              + " seq -s ' ' 10000000 10000910;"
              + " while true; do sleep 1; done" },
      waitFor = @WaitFor(value = "Kind started k8s cluster", timeout = 300_000),
      environments = { @Environment(key = "DOCKER_HOST", value = "tcp://172.17.0.1:2376") },
      mounts = {
          @Mount(path = "/scripts", value = "/restart-kind.sh"),
      })
})
public class StackGresOperatorIt extends AbstractStackGresOperatorIt {

  private final String CLUSTER_NAME = "test";

  @Test
  public void createClusterTest(@ContainerParam("kind") Container kind) throws Exception {
    ItHelper.installStackGresConfigs(kind, namespace);
    ItHelper.installStackGresCluster(kind, namespace, CLUSTER_NAME);
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("bash", "-l", "-c",
        "kubectl get pod -n  " + namespace + " " + CLUSTER_NAME + "-0"
            + " && echo 1 || true")),
        s -> !s.noneMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking creation of"
                + " pod '" + CLUSTER_NAME + "-0' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("bash", "-l", "-c",
        "kubectl describe pod -n  " + namespace + " " + CLUSTER_NAME + "-0")),
        s -> !s.noneMatch(line -> line.matches("Status:\\s+Running")), 120, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking availability of"
                + " pod '" + CLUSTER_NAME + "-0' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("bash", "-l", "-c",
        "kubectl exec -t -n " + namespace + " " + CLUSTER_NAME + "-0 -c postgres-util --"
            + " bash -c \"psql -t -A -U postgres -p 5432 -c 'SELECT 1' || true\"")),
        s -> !s.noneMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking connection available to postgres of"
                + " pod '" + CLUSTER_NAME + "-0' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
    ItHelper.waitUntil(Unchecked.supplier(() -> kind.execute("bash", "-l", "-c",
        "kubectl exec -t -n " + namespace + " " + CLUSTER_NAME + "-0 -c postgres-util --"
            + " bash -c \"PGPASSWORD=$(kubectl get secret " + CLUSTER_NAME + " -n " + namespace
            + " -o yaml | grep superuser-password | cut -d ':' -f 2 | tr -d ' ' | base64 -d)"
            + " psql -t -A -U postgres -d postgres -p 6432 -c 'SELECT 1' || true\"")),
        s -> !s.noneMatch(line -> line.equals("1")), 60, ChronoUnit.SECONDS,
        s -> Assertions.fail(
            "Timeout while checking connection available to pgbouncer of"
                + " pod '" + CLUSTER_NAME + "-0' in namespace '" + namespace + "':\n"
                + s.collect(Collectors.joining("\n"))));
  }
}
