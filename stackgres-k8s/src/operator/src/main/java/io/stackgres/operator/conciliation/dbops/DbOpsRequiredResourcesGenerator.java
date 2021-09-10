/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.DecorateResource;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(DbOpsRequiredResourcesGenerator.class);

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final DecorateResource<StackGresDbOpsContext> decorator;

  @Inject
  public DbOpsRequiredResourcesGenerator(
      CustomResourceFinder<StackGresCluster> clusterFinder,
      DecorateResource<StackGresDbOpsContext> decorator) {
    this.clusterFinder = clusterFinder;
    this.decorator = decorator;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDbOps config) {
    final ObjectMeta metadata = config.getMetadata();
    final String dbOpsName = metadata.getName();
    final String dbOpsNamespace = metadata.getNamespace();

    final StackGresDbOpsSpec spec = config.getSpec();
    final StackGresCluster cluster = clusterFinder
        .findByNameAndNamespace(spec.getSgCluster(), dbOpsNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGDbOps " + dbOpsNamespace + "/" + dbOpsName
                + " have a non existent SGCluster " + spec.getSgCluster()));

    StackGresDbOpsContext context = ImmutableStackGresDbOpsContext.builder()
        .source(config)
        .cluster(cluster)
        .build();

    return decorator.decorateResources(context);
  }

}
