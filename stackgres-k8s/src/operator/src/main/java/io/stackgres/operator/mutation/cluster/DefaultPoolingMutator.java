/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultPoolingMutator
    extends AbstractDefaultResourceMutator<StackGresPoolingConfig>
    implements ClusterMutator {

  @Inject
  public DefaultPoolingMutator(
      DefaultCustomResourceFactory<StackGresPoolingConfig> resourceFactory,
      CustomResourceFinder<StackGresPoolingConfig> finder,
      CustomResourceScheduler<StackGresPoolingConfig> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultPoolingMutator() {
    super(null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {

    if (review.getRequest().getOperation() == Operation.CREATE) {

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(ClusterConfigurationMutator.ensureConfigurationNode(review));
      operations.addAll(super.mutate(review));
      return operations.build();
    }

    return ImmutableList.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresCluster targetCluster) {
    return targetCluster.getSpec().getConfiguration().getConnectionPoolingConfig();
  }

  @Override
  protected boolean applyDefault(StackGresCluster targetCluster) {

    return Optional.ofNullable(targetCluster.getSpec())
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getDisableConnectionPooling)
        .map(d -> super.applyDefault(targetCluster))
        .orElse(false);
  }

  @Override
  protected JsonPointer getTargetPointer() throws NoSuchFieldException {
    return getTargetPointer("connectionPoolingConfig");
  }

  @Override
  public JsonPointer getTargetPointer(String field) throws NoSuchFieldException {
    return ClusterConfigurationMutator.getTargetPointer(field);
  }
}
