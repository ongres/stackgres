/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClusterConfigContextAppender
    extends ContextAppender<StackGresCluster, Builder> {

  private final CustomResourceScanner<StackGresConfig> configScanner;

  public ClusterConfigContextAppender(CustomResourceScanner<StackGresConfig> configScanner) {
    this.configScanner = configScanner;
  }

  @Override
  public void appendContext(StackGresCluster cluster, Builder contextBuilder) {
    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            StackGresConfig.KIND + " not found or more than one exists. Aborting reoconciliation!"));
    contextBuilder.config(config);
  }

}
