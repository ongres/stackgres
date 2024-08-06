/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Authenticated
public abstract class AbstractNamespacedRestService
    <T extends ResourceDto, R extends CustomResource<?, ?>>
    implements ResourceNamespacedRestService<T> {

  @Inject
  CustomResourceFinder<R> finder;

  @Inject
  ResourceTransformer<T, R> transformer;

  public CustomResourceFinder<R> getFinder() {
    return finder;
  }

  public ResourceTransformer<T, R> getTransformer() {
    return transformer;
  }

  /**
   * Look for a specific resource based on it's namespace and name.
   *
   * @param namespace the namespace in which the resource is located
   * @param name the resource name
   * @return the founded resource
   * @throws NotFoundException if no resource is found
   */
  @GET
  @Path("{name}")
  @Override
  public T get(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return finder.findByNameAndNamespace(name, namespace)
        .map(transformer::toDto)
        .orElseThrow(NotFoundException::new);
  }

}
