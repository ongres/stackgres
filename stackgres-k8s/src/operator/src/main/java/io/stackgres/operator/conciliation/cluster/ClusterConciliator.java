/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResource;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterConciliator extends AbstractConciliator<StackGresCluster> {

  private final LabelFactoryForCluster labelFactory;
  private final PatroniCtl patroniCtl;

  @Inject
  public ClusterConciliator(
      KubernetesClient client,
      CustomResourceFinder<StackGresCluster> finder,
      RequiredResourceGenerator<StackGresCluster> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresCluster> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache,
      LabelFactoryForCluster labelFactory,
      PatroniCtl patroniCtl) {
    super(client, finder, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
    this.labelFactory = labelFactory;
    this.patroniCtl = patroniCtl;
  }

  @Override
  protected boolean skipDeletion(DeployedResource deployedResource, StackGresCluster config) {
    if (deployedResource.apiVersion().equals(HasMetadata.getApiVersion(Pod.class))
        && deployedResource.kind().equals(HasMetadata.getKind(Pod.class))
        && deployedResource.name().startsWith(
            config.getMetadata().getName() + "-")) {
      return true;
    }
    if (deployedResource.apiVersion().equals(HasMetadata.getApiVersion(StackGresBackup.class))
        && deployedResource.kind().equals(HasMetadata.getKind(StackGresBackup.class))
        && !deployedResource.hasDeployedLabels(
            Map.of(
                StackGresContext.STACKGRES_KEY_PREFIX
                + StackGresContext.RECONCILIATION_INITIALIZATION_BACKUP_KEY,
                StackGresContext.RIGHT_VALUE))) {
      return true;
    }
    return false;
  }

  @Override
  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  protected boolean forceChange(HasMetadata requiredResource, StackGresCluster config) {
    if (requiredResource instanceof StatefulSet requiredStatefulSet
        && requiredStatefulSet.getMetadata().getName().equals(
            config.getMetadata().getName())) {
      var patroniCtl = this.patroniCtl.instanceFor(config);
      final Boolean isPatroniOnKubernetes = Optional.ofNullable(config.getSpec().getConfigurations())
          .map(StackGresClusterConfigurations::getPatroni)
          .map(StackGresClusterPatroni::getInitialConfig)
          .map(StackGresClusterPatroniConfig::isPatroniOnKubernetes)
          .orElse(true);
      Map<String, String> primaryLabels =
          labelFactory.clusterPrimaryLabelsWithoutUidAndScope(config);
      final boolean noPrimaryPod =
          (isPatroniOnKubernetes
              || patroniCtl.list()
              .stream()
              .noneMatch(member -> member.isPrimary()
                  && !member.getMember().startsWith(config.getMetadata().getName() + "-")))
          && deployedResourcesCache
          .stream()
          .noneMatch(deployedResource -> isPrimaryPod(deployedResource, primaryLabels));
      if (noPrimaryPod && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since no primary pod with labels {} was"
            + " found for SGCluster {}.{}",
            primaryLabels,
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      final boolean anyPodWithWrongOrMissingRole;
      if (!isPatroniOnKubernetes) {
        var members = patroniCtl.list();
        anyPodWithWrongOrMissingRole = deployedResourcesCache
            .stream()
            .anyMatch(deployedResource -> isPodWithWrongOrMissingRole(config, deployedResource, members));
      } else {
        anyPodWithWrongOrMissingRole = false;
      }
      if (anyPodWithWrongOrMissingRole && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since some pod with wrong or missing role label was"
            + " found for SGCluster {}.{}",
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      return noPrimaryPod || anyPodWithWrongOrMissingRole;
    }
    return false;
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private boolean isPrimaryPod(
      DeployedResource deployedResource,
      Map<String, String> primaryLabels) {
    return deployedResource.apiVersion().equals(HasMetadata.getApiVersion(Pod.class))
        && deployedResource.kind().equals(HasMetadata.getKind(Pod.class))
        && deployedResource.hasDeployedLabels(primaryLabels);
  }

  @SuppressFBWarnings(value = "SA_LOCAL_SELF_COMPARISON",
      justification = "False positive")
  private boolean isPodWithWrongOrMissingRole(
      StackGresCluster config,
      DeployedResource deployedResource,
      List<PatroniMember> members) {
    final String patroniVersion = StackGresUtil.getPatroniVersion(config);
    final int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    return deployedResource.apiVersion().equals(HasMetadata.getApiVersion(Pod.class))
        && deployedResource.kind().equals(HasMetadata.getKind(Pod.class))
        && !deployedResource.hasDeployedLabels(
            Map.of(
                PatroniUtil.ROLE_KEY,
                members.stream()
                .filter(member -> deployedResource.name().equals(member.getMember())
                    && member.getLabelRole(patroniMajorVersion) != null)
                .map(member -> member.getLabelRole(patroniMajorVersion))
                .findFirst()
                .orElse(null)));
  }

}
