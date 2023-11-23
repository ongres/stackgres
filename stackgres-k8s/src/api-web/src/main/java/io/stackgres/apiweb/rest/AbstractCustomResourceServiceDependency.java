/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.QueryParam;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Authenticated
public abstract class AbstractCustomResourceServiceDependency
    <T extends ResourceDto, R extends CustomResource<?, ?>>
    implements ResourceRestService<T> {

  @Inject
  CustomResourceScanner<R> scanner;

  @Inject
  CustomResourceFinder<R> finder;

  @Inject
  CustomResourceScheduler<R> scheduler;

  @Inject
  CustomResourceScanner<StackGresCluster> clusterScanner;

  @Inject
  DependencyResourceTransformer<T, R> transformer;

  public abstract boolean belongsToCluster(R resource, StackGresCluster cluster);

  /**
   * Looks for all resources of type {@code <R>} that are installed in the kubernetes cluster.
   *
   * @return a list with the installed resources
   * @throws RuntimeException if no custom resource of type {@code <R>} is defined
   */
  @GET
  @CommonApiResponses
  @Override
  public List<T> list() {
    final List<R> resources = scanner.getResources();

    List<StackGresCluster> clusters = clusterScanner.getResources();

    return resources.stream()
        .map(resource -> transformer.toResource(
            resource,
            connectedClusters(clusters, resource)))
        .collect(Collectors.toList());
  }

  private List<String> connectedClusters(List<StackGresCluster> clusters, R resource) {
    return Seq.seq(clusters).filter(cluster -> belongsToCluster(resource, cluster))
        .map(cluster ->
            StackGresUtil.getRelativeId(
                cluster.getMetadata().getName(),
                cluster.getMetadata().getNamespace(),
                resource.getMetadata().getNamespace())
        )
        .toList();
  }

  /**
   * Creates a resource of type {@code <R>}.
   *
   * @param resource the resource to create
   */
  @POST
  @CommonApiResponses
  @Override
  public T create(@NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    return transformer.toResource(
        scheduler.create(transformer.toCustomResource(resource, null),
            Optional.ofNullable(dryRun).orElse(false)),
        List.of());
  }

  /**
   * Deletes a custom resource of type {@code <R>}.
   *
   * @param resource the resource to delete
   */
  @DELETE
  @CommonApiResponses
  @Override
  public void delete(@NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    scheduler.delete(transformer.toCustomResource(resource, null),
        Optional.ofNullable(dryRun).orElse(false));
  }

  /**
   * Updates a custom resource of type {@code <R>}.
   *
   * @param resource the resource to delete
   */
  @PUT
  @CommonApiResponses
  @Override
  public T update(@NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    R transformedResource = transformer.toCustomResource(
        resource,
        finder.findByNameAndNamespace(
            resource.getMetadata().getName(), resource.getMetadata().getNamespace())
            .orElseThrow(NotFoundException::new));

    List<StackGresCluster> clusters = clusterScanner.getResources();

    if (Optional.ofNullable(dryRun).orElse(false)) {
      return transformer.toResource(
          scheduler.update(
              transformedResource,
              Optional.ofNullable(dryRun).orElse(false)),
          connectedClusters(clusters, transformedResource));
    }
    return transformer.toResource(
        scheduler.update(transformedResource,
            currentResource -> updateSpec(currentResource, transformedResource)),
        connectedClusters(clusters, transformedResource));
  }

  protected abstract void updateSpec(R resourceToUpdate, R resource);

}
