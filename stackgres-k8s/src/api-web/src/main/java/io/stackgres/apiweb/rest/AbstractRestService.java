/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.ResourceTransformer;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jooq.lambda.Seq;

public abstract class AbstractRestService<T extends ResourceDto, R extends CustomResource>
    implements ResourceRestService<T> {

  private final CustomResourceScanner<R> scanner;
  private final CustomResourceFinder<R> finder;
  private final CustomResourceScheduler<R> scheduler;
  private final ResourceTransformer<T, R> transformer;

  public AbstractRestService(CustomResourceScanner<R> scanner,
      CustomResourceFinder<R> finder, CustomResourceScheduler<R> scheduler,
      ResourceTransformer<T, R> transformer) {
    super();
    this.scanner = scanner;
    this.finder = finder;
    this.scheduler = scheduler;
    this.transformer = transformer;
  }

  /**
   * Looks for all resources of type <code>{R}</code> that are installed in the kubernetes cluster.
   * @return a list with the installed resources
   * @throws RuntimeException if no custom resource of type <code>{R}</code> is defined
   */
  @GET
  @Authenticated
  @Override
  public List<T> list() {
    return Seq.seq(scanner.getResources())
        .map(transformer::toDto)
        .toList();
  }

  /**
   * Look for a specific resource based on it's namespace and name.
   * @param namespace the namespace in which the resource is located
   * @param name the resource name
   * @return the founded resource
   * @throws NotFoundException if no resource is found
   */
  @Path("/{namespace}/{name}")
  @GET
  @Authenticated
  @Override
  public T get(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return finder.findByNameAndNamespace(name, namespace)
        .map(transformer::toDto)
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Creates a resource of type <code>{R}</code>.
   * @param resource the resource to create
   */
  @POST
  @Authenticated
  @Override
  public void create(T resource) {
    scheduler.create(transformer.toCustomResource(resource, null));
  }

  /**
   * Deletes a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @DELETE
  @Authenticated
  @Override
  public void delete(T resource) {
    scheduler.delete(transformer.toCustomResource(resource, null));
  }

  /**
   * Updates a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @PUT
  @Authenticated
  @Override
  public void update(T resource) {
    scheduler.update(transformer.toCustomResource(resource,
        finder.findByNameAndNamespace(
            resource.getMetadata().getName(), resource.getMetadata().getNamespace())
        .orElseThrow(NotFoundException::new)));
  }

  public CustomResourceScanner<R> getScanner() {
    return scanner;
  }

  public CustomResourceFinder<R> getFinder() {
    return finder;
  }

  public CustomResourceScheduler<R> getScheduler() {
    return scheduler;
  }

  public ResourceTransformer<T, R> getTransformer() {
    return transformer;
  }
}
