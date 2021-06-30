/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import javax.inject.Inject;
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
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jooq.lambda.Seq;

@Path("")
public abstract class AbstractDependencyRestService
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
   * Looks for all resources of type <code>{R}</code> that are installed in the kubernetes cluster.
   * @return a list with the installed resources
   * @throws RuntimeException if no custom resource of type <code>{R}</code> is defined
   */
  @GET
  @Authenticated
  @CommonApiResponses
  @Override
  public List<T> list() {
    List<StackGresCluster> clusters = clusterScanner.getResources();
    return Seq.seq(scanner.getResources())
        .map(resource -> transformer.toResource(resource, Seq.seq(clusters)
            .filter(cluster -> belongsToCluster(resource, cluster))
            .map(cluster -> StackGresUtil.getRelativeId(
                cluster.getMetadata().getName(),
                cluster.getMetadata().getNamespace(),
                resource.getMetadata().getNamespace()))
            .toList()))
        .toList();
  }

  /**
   * Look for a specific resource based on it's namespace and name.
   * @param namespace the namespace in which the resource is located
   * @param name the resource name
   * @return the founded resource
   * @throws NotFoundException if no resource is found
   */
  @GET
  @Authenticated
  @CommonApiResponses
  @Override
  public T get(@PathParam("namespace") String namespace, @PathParam("name") String name) {
    List<StackGresCluster> clusters = clusterScanner.getResources();
    return finder.findByNameAndNamespace(name, namespace)
        .map(resource -> transformer.toResource(resource, Seq.seq(clusters)
            .filter(cluster -> belongsToCluster(resource, cluster))
            .map(cluster -> StackGresUtil.getRelativeId(
                cluster.getMetadata().getName(),
                cluster.getMetadata().getNamespace(),
                resource.getMetadata().getNamespace()))
            .toList()))
        .orElseThrow(NotFoundException::new);
  }

  /**
   * Creates a resource of type <code>{R}</code>.
   * @param resource the resource to create
   */
  @POST
  @Authenticated
  @CommonApiResponses
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
  @CommonApiResponses
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
  @CommonApiResponses
  @Override
  public void update(T resource) {
    scheduler.update(transformer.toCustomResource(resource,
        finder.findByNameAndNamespace(
            resource.getMetadata().getName(), resource.getMetadata().getNamespace())
        .orElseThrow(NotFoundException::new)));
  }
}
