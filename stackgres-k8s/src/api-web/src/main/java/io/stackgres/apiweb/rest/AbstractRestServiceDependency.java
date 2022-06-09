/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

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
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Authenticated
public abstract class AbstractRestServiceDependency
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
            Seq.seq(clusters).filter(cluster -> belongsToCluster(resource, cluster))
                .map(cluster ->
                    StackGresUtil.getRelativeId(
                        cluster.getMetadata().getName(),
                        cluster.getMetadata().getNamespace(),
                        resource.getMetadata().getNamespace())
                )
                .toList()))
        .collect(Collectors.toList());
  }

  /**
   * Creates a resource of type {@code <R>}.
   *
   * @param resource the resource to create
   */
  @POST
  @CommonApiResponses
  @Override
  public void create(@NotNull T resource) {
    scheduler.create(transformer.toCustomResource(resource, null));
  }

  /**
   * Deletes a custom resource of type {@code <R>}.
   *
   * @param resource the resource to delete
   */
  @DELETE
  @CommonApiResponses
  @Override
  public void delete(@NotNull T resource) {
    scheduler.delete(transformer.toCustomResource(resource, null));
  }

  /**
   * Updates a custom resource of type {@code <R>}.
   *
   * @param resource the resource to delete
   */
  @PUT
  @CommonApiResponses
  @Override
  public void update(@NotNull T resource) {
    scheduler.update(transformer.toCustomResource(resource,
        finder.findByNameAndNamespace(
                resource.getMetadata().getName(), resource.getMetadata().getNamespace())
            .orElseThrow(NotFoundException::new)), this::updateSpec);
  }

  protected abstract void updateSpec(R resourceToUpdate, R resource);

}
