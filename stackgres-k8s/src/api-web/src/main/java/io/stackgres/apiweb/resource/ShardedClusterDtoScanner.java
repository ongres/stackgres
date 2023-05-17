/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.apiweb.transformer.ShardedClusterTransformer;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedClusterDtoScanner implements CustomResourceScanner<ShardedClusterDto> {

  private CustomResourceScanner<StackGresShardedCluster> shardedClusterScanner;
  private ShardedClusterTransformer shardedClusterTransformer;

  @Override
  public List<ShardedClusterDto> getResources() {
    return Seq.seq(shardedClusterScanner.getResources())
        .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
        .toList();
  }

  @Override
  public List<ShardedClusterDto> getResources(String namespace) {
    return Seq.seq(shardedClusterScanner.getResources(namespace))
        .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
        .toList();
  }

  @Override
  public List<ShardedClusterDto> getResourcesWithLabels(
      Map<String, String> labels) {
    return Seq.seq(shardedClusterScanner.getResourcesWithLabels(labels))
        .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
        .toList();
  }

  @Override
  public List<ShardedClusterDto> getResourcesWithLabels(
      String namespace, Map<String, String> labels) {
    return Seq.seq(shardedClusterScanner.getResourcesWithLabels(namespace, labels))
        .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
        .toList();
  }

  @Override
  public Optional<List<ShardedClusterDto>> findResources() {
    return shardedClusterScanner.findResources()
        .map(resources -> Seq.seq(resources)
            .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
            .toList());
  }

  @Override
  public Optional<List<ShardedClusterDto>> findResources(String namespace) {
    return shardedClusterScanner.findResources(namespace)
        .map(resources -> Seq.seq(resources)
            .map(resrouce -> shardedClusterTransformer.toResource(resrouce, List.of()))
            .toList());
  }

  @Inject
  public void setShardedClusterScanner(
      CustomResourceScanner<StackGresShardedCluster> shardedClusterScanner) {
    this.shardedClusterScanner = shardedClusterScanner;
  }

  @Inject
  public void setShardedClusterTransformer(ShardedClusterTransformer shardedClusterTransformer) {
    this.shardedClusterTransformer = shardedClusterTransformer;
  }

}
