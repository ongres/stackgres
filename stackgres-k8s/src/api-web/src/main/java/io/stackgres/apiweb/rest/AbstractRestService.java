/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Authenticated
public abstract class AbstractRestService
    <T extends ResourceDto, R extends CustomResource<?, ?>>
    implements ResourceRestService<T> {

  @Inject
  CustomResourceScanner<R> scanner;

  @Inject
  CustomResourceFinder<R> finder;

  @Inject
  CustomResourceScheduler<R> scheduler;

  @Inject
  ResourceTransformer<T, R> transformer;

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
    return Seq.seq(scanner.getResources())
        .map(transformer::toDto)
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
    R transformedResource = transformer.toCustomResource(
        resource,
        finder.findByNameAndNamespace(
            resource.getMetadata().getName(), resource.getMetadata().getNamespace())
            .orElseThrow(NotFoundException::new));
    scheduler.update(transformedResource,
        currentResource -> updateSpec(currentResource, transformedResource));
  }

  protected abstract void updateSpec(R resourceToUpdate, R resource);

}
