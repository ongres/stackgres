/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.rest.dto.Resource;
import io.stackgres.operator.rest.transformer.ResourceTransformer;

import org.jooq.lambda.Seq;

public class AbstractRestService<T extends Resource, R extends CustomResource>
    implements ResourceRestService<T> {

  private final CustomResourceScanner<R> scanner;
  private final CustomResourceFinder<R> finder;
  private final CustomResourceScheduler<R> scheduler;
  private final ResourceTransformer<T, R> transformer;

  AbstractRestService(CustomResourceScanner<R> scanner,
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
  @Override
  @GET
  public List<T> list() {
    return Seq.seq(scanner.getResources())
        .map(transformer::toResource)
        .toList();
  }

  /**
   * Look for a specific resource based on it's namespace and name.
   * @param namespace the namespace in which the resource is located
   * @param name the resource name
   * @return the founded resource
   * @throws NotFoundException if no resource is found
   */
  @Override
  @Path("/{namespace}/{name}")
  @GET
  public T get(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return finder.findByNameAndNamespace(name, namespace)
        .map(transformer::toResource)
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Creates a resource of type <code>{R}</code>.
   * @param resource the resource to create
   */
  @Override
  @POST
  public void create(T resource) {
    scheduler.create(transformer.toCustomResource(resource));
  }

  /**
   * Deletes a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @Override
  @DELETE
  public void delete(T resource) {
    scheduler.delete(transformer.toCustomResource(resource));
  }

  /**
   * Updates a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @Override
  @PUT
  public void update(T resource) {
    scheduler.update(transformer.toCustomResource(resource));
  }
}
