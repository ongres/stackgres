/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import io.stackgres.operator.patroni.StackGresStatefulSet;
import io.stackgres.operator.resource.dto.ClusterResourceConsumtion;
import io.stackgres.operator.rest.PatroniStatsScripts;
import okhttp3.Response;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ClusterResourceConsumptionFinder
    implements KubernetesCustomResourceFinder<ClusterResourceConsumtion> {

  @Inject
  KubernetesCustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  KubernetesClientFactory kubClientFactory;

  public ClusterResourceConsumptionFinder(
      KubernetesClientFactory kubClientFactory,
      KubernetesCustomResourceFinder<StackGresCluster> clusterFinder) {
    this.kubClientFactory = kubClientFactory;
    this.clusterFinder = clusterFinder;
  }

  public ClusterResourceConsumptionFinder() {
  }

  @Override
  public Optional<ClusterResourceConsumtion> findByNameAndNamespace(String name, String namespace) {

    return clusterFinder.findByNameAndNamespace(name, namespace).map(cluster -> {

      ClusterResourceConsumtion status = new ClusterResourceConsumtion();

      try (KubernetesClient client = kubClientFactory.create()) {

        Optional<Pod> masterPod = client.pods()
            .inNamespace(cluster.getMetadata().getNamespace())
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
                .put("role", "master")
                .build())
            .list()
            .getItems()
            .stream()
            .findAny();

        ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME)
            .flatMap(crd -> Optional.ofNullable(client.customResources(crd,
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
                .inNamespace(cluster.getMetadata().getNamespace())
                .withName(cluster.getSpec().getResourceProfile())
                .get()))
            .ifPresent(profile -> {
              status.setCpuRequested(profile.getSpec().getCpu());
              status.setMemoryRequested(profile.getSpec().getMemory());
            });

        status.setCpuFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getCpuFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setMemoryFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getMemoryFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setMemoryUsed(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getMemoryUsed())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setDiskFound(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getDiskFound())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setDiskUsed(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getDiskUsed())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad1m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad1m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad5m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad5m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        status.setAverageLoad10m(masterPod
            .map(Unchecked.function(pod -> exec(
                client, pod,
                "sh", "-c", PatroniStatsScripts.getLoad10m())
                .stream()
                .findAny()
                .orElse(null)))
            .orElse(null));

        return status;
      }
    });

  }

  private List<String> exec(KubernetesClient client, Pod pod, String... args)
      throws Exception {
    CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
         ByteArrayOutputStream errorCodeStream = new ByteArrayOutputStream();
         ExecWatch ignored = client.pods()
             .inNamespace(pod.getMetadata().getNamespace())
             .withName(pod.getMetadata().getName())
             .inContainer(StackGresStatefulSet.PATRONI_CONTAINER_NAME)
             .writingOutput(outputStream)
             .writingError(errorStream)
             .writingErrorChannel(errorCodeStream)
             .usingListener(new ExecListener() {
               @Override
               public void onOpen(Response response) {
               }

               @Override
               public void onFailure(Throwable t, Response response) {
                 completableFuture.completeExceptionally(t);
               }

               @Override
               public void onClose(int code, String reason) {
                 try {
                   outputStream.write(errorStream.toByteArray());
                   Status status = Serialization.unmarshal(
                       new String(errorCodeStream.toByteArray(), Charsets.UTF_8), Status.class);

                   int exitCode = status.getStatus().equals("Success") ? 0
                       : Integer.parseInt(status.getDetails().getCauses().stream()
                       .filter(cause -> cause.getReason() != null)
                       .filter(cause -> cause.getReason().equals("ExitCode"))
                       .map(StatusCause::getMessage)
                       .findFirst().orElse("-1"));
                   if (exitCode != 0) {
                     try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                         outputStream.toByteArray());
                          InputStreamReader inputStreamReader = new InputStreamReader(
                              byteArrayInputStream, Charsets.UTF_8);
                          BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                       completableFuture.completeExceptionally(new RuntimeException(
                           "Command exited with code " + exitCode + " on container "
                               + StackGresStatefulSet.PATRONI_CONTAINER_NAME
                               + " of pod " + pod.getMetadata().getName()
                               + " in namespace " + pod.getMetadata().getNamespace()
                               + " with arguments " + Arrays.asList(args) + ": "
                               + status.getDetails().getCauses().stream()
                               .filter(cause -> cause.getMessage() != null)
                               .map(StatusCause::getMessage)
                               .findFirst().orElse("Unknown cause") + "\n"
                               + bufferedReader.lines().collect(Collectors.joining("\n"))));
                     }
                   }

                   completableFuture.complete(null);
                 } catch (Exception ex) {
                   completableFuture.completeExceptionally(ex);
                 }
               }
             })
             .exec(args)) {
      completableFuture.join();

      try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
          outputStream.toByteArray());
           InputStreamReader inputStreamReader = new InputStreamReader(
               byteArrayInputStream, Charsets.UTF_8);
           BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
        return bufferedReader.lines().collect(Collectors.toList());
      }
    }
  }
}
