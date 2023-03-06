/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ClusterLabelFactory extends AbstractLabelFactoryForCluster<StackGresCluster> {

  private final ClusterLabelMapper labelMapper;

  @Inject
  public ClusterLabelFactory(ClusterLabelMapper labelMapper) {
    this.labelMapper = labelMapper;
  }

  @Override
  public ClusterLabelMapper labelMapper() {
    return labelMapper;
  }

  @Override
  public String resourceScope(@NotNull StackGresCluster resource) {
    return Optional.ofNullable(resource.getSpec().getConfiguration().getPatroni())
        .map(patroni -> patroni.getInitialConfig())
        .map(patroniConfig -> patroniConfig.getScope())
        .orElse(resourceName(resource));
  }

  @Override
  public Map<String, String> patroniReplicaLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(super.patroniReplicaLabels(resource))
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

}
