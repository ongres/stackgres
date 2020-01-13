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
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

public class AbstractCustomResourceRestService<R extends CustomResource>
    implements CustomResourceRestService<R> {

  private final KubernetesCustomResourceScanner<R> scanner;
  private final KubernetesCustomResourceFinder<R> finder;
  private final CustomResourceScheduler<R> scheduler;

  AbstractCustomResourceRestService(KubernetesCustomResourceScanner<R> scanner,
      KubernetesCustomResourceFinder<R> finder, CustomResourceScheduler<R> scheduler) {
    super();
    this.scanner = scanner;
    this.finder = finder;
    this.scheduler = scheduler;
  }

  /**
   * Looks for all resources of type <code>{R}</code> that are installed in the kubernetes cluster.
   * @return a list with the installed resources
   * @throws RuntimeException if no custom resource of type <code>{R}</code> is defined
   */
  @Override
  @GET
  public List<R> list() {
    return scanner.getResources();
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
  public R get(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    return finder.findByNameAndNamespace(name, namespace)
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Creates a resource of type <code>{R}</code>.
   * @param resource the resource to create
   */
  @Override
  @POST
  public void create(R resource) {
    scheduler.create(resource);
  }

  /**
   * Deletes a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @Override
  @DELETE
  public void delete(R resource) {
    scheduler.delete(resource);
  }

  /**
   * Updates a custom resource of type <code>{R}</code>.
   * @param resource the resource to delete
   */
  @Override
  @PUT
  public void update(R resource) {
    scheduler.update(resource);
  }
}
