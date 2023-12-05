/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.QueryParam;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;

@Authenticated
public abstract class AbstractResourceService
    <T extends ResourceDto, R extends HasMetadata>
    implements ValidatedResourceRestService<T> {

  @Inject
  protected  ResourceScanner<R> scanner;

  @Inject
  protected  ResourceFinder<R> finder;

  @Inject
  protected  ResourceWriter<R> writer;

  @Inject
  protected  ResourceTransformer<T, R> transformer;

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
    return Seq.seq(scanner.findResources())
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
  public T create(@Valid @NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    return transformer.toDto(
        writer.create(transformer.toCustomResource(resource, null),
        Optional.ofNullable(dryRun).orElse(false)));
  }

  /**
   * Deletes a custom resource of type {@code <R>}.
   *
   * @param resource the resource to delete
   */
  @DELETE
  @CommonApiResponses
  @Override
  public void delete(@Valid @NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    writer.delete(transformer.toCustomResource(resource, null),
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
  public T update(@Valid @NotNull T resource, @Nullable @QueryParam("dryRun") Boolean dryRun) {
    R transformedResource = transformer.toCustomResource(
        resource,
        findResource(resource)
            .orElseThrow(NotFoundException::new));
    if (Optional.ofNullable(dryRun).orElse(false)) {
      return transformer.toDto(writer.update(
          transformedResource,
          Optional.ofNullable(dryRun).orElse(false)));
    }
    return transformer.toDto(writer.update(transformedResource,
        currentResource -> updateSpec(currentResource, transformedResource)));
  }

  protected Optional<R> findResource(T resource) {
    return finder.findByNameAndNamespace(
        resource.getMetadata().getName(), resource.getMetadata().getNamespace());
  }

  protected abstract void updateSpec(R resourceToUpdate, R resource);

}
