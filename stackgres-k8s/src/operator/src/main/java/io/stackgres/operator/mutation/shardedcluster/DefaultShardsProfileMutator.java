/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShards;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultShardsProfileMutator
    extends AbstractDefaultResourceMutator<
        StackGresProfile, StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  @Inject
  public DefaultShardsProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresShardedClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      final StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
      final StackGresShardedClusterShards shards = spec.getShards();
      StackGresClusterPod pod = shards.getPod();
      final JsonPointer clusterPodPointer = SPEC_POINTER
          .append("pod");
      if (pod == null) {
        pod = new StackGresClusterPod();
        shards.setPod(pod);
        operations.add(new AddOperation(clusterPodPointer, FACTORY.objectNode()));
      }
      if (pod.getPersistentVolume() == null) {
        operations.add(new AddOperation(clusterPodPointer
            .append("persistentVolume"), FACTORY.objectNode()));
      }
      operations.addAll(super.mutate(review));
      return operations.build();
    }
    return ImmutableList.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresShardedCluster targetCluster) {
    return targetCluster.getSpec().getShards().getResourceProfile();
  }

  @Override
  protected JsonPointer getTargetPointer() {
    return getShardsTargetPointer("resourceProfile");
  }
}
