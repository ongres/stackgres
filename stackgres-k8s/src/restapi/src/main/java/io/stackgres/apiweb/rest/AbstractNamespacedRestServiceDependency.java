/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.transformer.DependencyResourceTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.jooq.lambda.Seq;

@Authenticated
public abstract class AbstractNamespacedRestServiceDependency
    <T extends ResourceDto, R extends CustomResource<?, ?>>
    implements ResourceNamespacedRestService<T> {

  @Inject
  CustomResourceFinder<R> finder;

  @Inject
  CustomResourceScanner<StackGresCluster> clusterScanner;

  @Inject
  DependencyResourceTransformer<T, R> transformer;

  public abstract boolean belongsToCluster(R resource, StackGresCluster cluster);

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

}
