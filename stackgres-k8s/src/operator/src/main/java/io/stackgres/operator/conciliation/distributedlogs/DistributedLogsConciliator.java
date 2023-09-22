/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResource;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class DistributedLogsConciliator extends AbstractConciliator<StackGresDistributedLogs> {

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public DistributedLogsConciliator(
      KubernetesClient client,
      RequiredResourceGenerator<StackGresDistributedLogs> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresDistributedLogs> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    super(client, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
    this.labelFactory = labelFactory;
  }

  @Override
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  protected boolean forceChange(HasMetadata requiredResource, StackGresDistributedLogs config) {
    if (requiredResource instanceof StatefulSet requiredStatefulSet
        && requiredStatefulSet.getMetadata().getName().equals(
            config.getMetadata().getName())) {
      Map<String, String> primaryLabels =
          labelFactory.clusterPrimaryLabelsWithoutUidAndScope(config);
      boolean result = deployedResourcesCache
          .stream()
          .map(DeployedResource::foundDeployed)
          .noneMatch(foundDeployedResource -> isPrimaryPod(foundDeployedResource, primaryLabels));
      if (result && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since no primary pod with labels {} was"
            + " found for SGDistributedLogs {}.{}",
            primaryLabels,
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      return result;
    }
    return false;
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private boolean isPrimaryPod(
      HasMetadata foundDeployedResource,
      Map<String, String> primaryLabels) {
    return foundDeployedResource instanceof Pod foundDeployedPod
        && Optional.of(foundDeployedPod.getMetadata())
        .map(ObjectMeta::getLabels)
        .filter(labels -> primaryLabels.entrySet().stream()
            .allMatch(primaryLabel -> labels.entrySet().stream()
                .anyMatch(primaryLabel::equals)))
        .isPresent();
  }

}
