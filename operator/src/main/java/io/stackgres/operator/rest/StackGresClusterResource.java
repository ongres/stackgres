/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.customresource.sgcluster.StackGresClusterDefinition;
import io.stackgres.common.customresource.sgcluster.StackGresClusterDoneable;
import io.stackgres.common.customresource.sgcluster.StackGresClusterList;
import io.stackgres.common.customresource.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.customresource.sgcluster.StackGresClusterStatus;
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.customresource.sgprofile.StackGresProfileList;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.patroni.StackGresStatefulSet;

import okhttp3.Response;

import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;

@Path("/stackgres/cluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StackGresClusterResource {

  @Inject
  KubernetesClientFactory kubeClient;

  /**
   * Return the list of {@code StackGresCluster}.
   */
  @GET
  public List<StackGresCluster> list() {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
          .map(crd -> client.customResources(crd,
              StackGresCluster.class,
              StackGresClusterList.class,
              StackGresClusterDoneable.class)
              .inAnyNamespace()
              .list()
              .getItems()
              .stream()
              .map(Unchecked.function(cluster -> withStatus(client, cluster)))
              .collect(Collectors.toList()))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
    }
  }

  /**
   * Return a {@code StackGresCluster}.
   */
  @Path("/{namespace}/{name}")
  @GET
  public StackGresCluster get(@PathParam("namespace") String namespace,
      @PathParam("name") String name) {
    try (KubernetesClient client = kubeClient.create()) {
      return ResourceUtil.getCustomResource(client, StackGresClusterDefinition.NAME)
          .map(crd -> Optional.ofNullable(client.customResources(crd,
              StackGresCluster.class,
              StackGresClusterList.class,
              StackGresClusterDoneable.class)
              .inNamespace(namespace)
              .withName(name)
              .get())
              .orElseThrow(() -> new NotFoundException()))
          .map(Unchecked.function(cluster -> withStatus(client, cluster)))
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
    }
  }

  private StackGresCluster withStatus(KubernetesClient client, StackGresCluster cluster)
      throws Exception {
    cluster.setStatus(new StackGresClusterStatus());

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
          cluster.getStatus().setCpuRequested(profile.getSpec().getCpu());
          cluster.getStatus().setMemoryRequested(profile.getSpec().getMemory());
        });

    cluster.getStatus().setCpuFound(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getCpuFound())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setMemoryFound(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getMemoryFound())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setMemoryUsed(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getMemoryUsed())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setDiskFound(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getDiskFound())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setDiskUsed(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getDiskUsed())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setAverageLoad1m(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getLoad1m())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setAverageLoad5m(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getLoad5m())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setAverageLoad10m(masterPod
        .map(Unchecked.function(pod -> exec(
            client, pod, StackGresStatefulSet.PATRONI_CONTAINER_NAME,
            "sh", "-c", PatroniStatsScripts.getLoad10m())
            .stream()
            .findAny()
            .orElse(null)))
        .orElse(null));

    cluster.getStatus().setPods(client.pods()
        .inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
        .list()
        .getItems()
        .stream()
        .map(pod -> Tuple.tuple(pod, new StackGresClusterPodStatus()))
        .peek(t -> t.v2.setNamespace(t.v1.getMetadata().getNamespace()))
        .peek(t -> t.v2.setName(t.v1.getMetadata().getName()))
        .peek(t -> t.v2.setRole(t.v1.getMetadata().getLabels().get("role")))
        .peek(t -> t.v2.setIp(t.v1.getStatus().getPodIP()))
        .peek(t -> t.v2.setStatus(t.v1.getStatus().getPhase()))
        .peek(t -> t.v2.setContainers(String.valueOf(t.v1.getSpec()
            .getContainers().size())))
        .peek(t -> t.v2.setContainersReady(String.valueOf(t.v1.getStatus()
            .getContainerStatuses()
            .stream()
            .filter(cs -> cs.getReady())
            .count())))
        .map(t -> t.v2)
        .collect(Collectors.toList()));

    cluster.getStatus().setPodsReady(String.valueOf(cluster.getStatus()
        .getPods()
        .stream()
        .filter(pod -> pod.getContainers().equals(pod.getContainersReady()))
        .count()));

    return cluster;
  }

  /**
   * Create a {@code StackGresCluster}.
   */
  @POST
  public void create(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
        .create(cluster);
    }
  }

  /**
   * Delete a {@code StackGresCluster}.
   */
  @DELETE
  public void delete(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
        .delete(cluster);
    }
  }

  /**
   * Create or update a {@code StackGresCluster}.
   */
  @PUT
  public void update(StackGresCluster cluster) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresClusterDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresClusterDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresCluster.class,
          StackGresClusterList.class,
          StackGresClusterDoneable.class)
        .createOrReplace(cluster);
    }
  }

  private List<String> exec(KubernetesClient client, Pod pod, String container, String... args)
      throws Exception {
    CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorCodeStream = new ByteArrayOutputStream();
        ExecWatch execWatch = client.pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .inContainer(container)
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
                          "Command exited with code " + exitCode + " on container " + container
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
