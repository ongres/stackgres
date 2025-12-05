/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResource;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
  protected boolean skipDeletion(HasMetadata foundDeployedResource, StackGresCluster config) {
    if (foundDeployedResource instanceof Pod foundDeployedResourcePod
        && foundDeployedResourcePod.getMetadata().getName().startsWith(
            config.getMetadata().getName() + "-")) {
      return true;
    }
    if (foundDeployedResource instanceof StackGresBackup foundDeployedResourceBackup
        && Optional.of(foundDeployedResourceBackup.getMetadata())
        .map(ObjectMeta::getLabels)
        .map(labels -> labels.get(
            StackGresContext.STACKGRES_KEY_PREFIX
            + StackGresContext.RECONCILIATION_INITIALIZATION_BACKUP_KEY))
        .filter(StackGresContext.RIGHT_VALUE::equals)
        .isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
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
      final List<PatroniMember> members = patroniCtl.list();
      final Map<String, String> primaryLabels =
          labelFactory.clusterPrimaryLabelsWithoutUidAndScope(config);
      final Map<String, String> clusterPodsLabels =
          labelFactory.clusterLabelsWithoutUidAndScope(config);
      final Map<String, String> genericLabels =
          labelFactory.genericLabels(config);
      final boolean noPrimaryPod =
          (isPatroniOnKubernetes
              || members
              .stream()
              .noneMatch(member -> member.isPrimary()
                  && !member.getMember().startsWith(config.getMetadata().getName() + "-")))
          && deployedResourcesCache
          .stream()
          .map(DeployedResource::foundDeployed)
          .noneMatch(foundDeployedResource -> isPodWithLabels(foundDeployedResource, primaryLabels));
      if (noPrimaryPod && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since no primary pod with labels {} was"
            + " found for SGCluster {}.{}",
            primaryLabels,
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      if (noPrimaryPod) {
        return true;
      }
      final boolean anyPodWithWrongOrMissingRole;
      if (!isPatroniOnKubernetes) {
        anyPodWithWrongOrMissingRole = deployedResourcesCache
            .stream()
            .map(DeployedResource::foundDeployed)
            .anyMatch(foundDeployedResource -> isPodWithWrongOrMissingRole(config, foundDeployedResource, members));
      } else {
        anyPodWithWrongOrMissingRole = false;
      }
      if (anyPodWithWrongOrMissingRole && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since some pod with wrong or missing role label was"
            + " found for SGCluster {}.{}",
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      if (anyPodWithWrongOrMissingRole) {
        return true;
      }
      final boolean anyPodCanRestart;
      if (ClusterRolloutUtil.isRolloutAllowed(config)) {
        anyPodCanRestart = Optional.of(config)
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getConditions)
            .stream()
            .flatMap(List::stream)
            .anyMatch(ClusterStatusCondition.POD_REQUIRES_RESTART::isCondition);
      } else {
        anyPodCanRestart = false;
      }
      if (anyPodCanRestart && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since some pod must be restarted for SGCluster {}.{}",
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      if (anyPodCanRestart) {
        return true;
      }
      final boolean podsCountMismatch = config.getSpec().getInstances()
          != deployedResourcesCache
              .stream()
              .map(DeployedResource::foundDeployed)
              .filter(foundDeployedResource -> isPodWithLabels(foundDeployedResource, clusterPodsLabels))
              .count();
      if (podsCountMismatch && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since pods count"
            + " mismatch with instances for SGCluster {}.{}",
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      if (podsCountMismatch) {
        return true;
      }
      final OwnerReference clusterOwnerReference = ResourceUtil.getOwnerReference(config);
      final boolean anyPodOrPvcWithMissingOwner = deployedResourcesCache
          .stream()
          .map(DeployedResource::foundDeployed)
          .filter(this::isPodOrPvc)
          .filter(foundDeployedResource -> hasLabels(genericLabels, foundDeployedResource))
          .anyMatch(foundDeployedResource -> isMissingOwner(
              foundDeployedResource, clusterOwnerReference));
      if (anyPodOrPvcWithMissingOwner && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Will force StatefulSet reconciliation since a pod or pvc is"
            + " missing owner reference for SGCluster {}.{}",
            config.getMetadata().getNamespace(),
            config.getMetadata().getName());
      }
      if (anyPodOrPvcWithMissingOwner) {
        return true;
      }
    }
    return false;
  }

  private boolean isPodWithLabels(
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

  private boolean isPodWithWrongOrMissingRole(
      StackGresCluster config,
      HasMetadata foundDeployedResource,
      List<PatroniMember> members) {
    final String patroniVersion = StackGresUtil.getPatroniVersion(config);
    final int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    return foundDeployedResource instanceof Pod foundDeployedPod
        && !Optional.of(foundDeployedPod.getMetadata())
        .map(ObjectMeta::getLabels)
        .filter(labels -> Objects.equals(
            members.stream()
            .filter(member -> foundDeployedPod.getMetadata().getName().equals(member.getMember())
                && member.getLabelRole(patroniMajorVersion) != null)
            .map(member -> member.getLabelRole(patroniMajorVersion))
            .findFirst()
            .orElse(null),
            labels.get(PatroniUtil.ROLE_KEY)))
        .isPresent();
  }

  private boolean isPodOrPvc(HasMetadata foundDeployedResource) {
    return foundDeployedResource instanceof Pod
        || foundDeployedResource instanceof PersistentVolumeClaim;
  }

  private boolean hasLabels(final Map<String, String> genericLabels, HasMetadata foundDeployedResource) {
    return genericLabels.entrySet().stream()
        .allMatch(genericLabel -> Optional
            .ofNullable(foundDeployedResource.getMetadata().getLabels())
            .map(Map::entrySet)
            .stream()
            .flatMap(Set::stream)
            .anyMatch(genericLabel::equals));
  }

  private boolean isMissingOwner(
      HasMetadata foundDeployedResource,
      OwnerReference clusterOwnerReference) {
    return !Optional.of(foundDeployedResource.getMetadata())
        .map(ObjectMeta::getOwnerReferences)
        .stream()
        .flatMap(List::stream)
        .anyMatch(ownerReference -> Objects.equals(
            clusterOwnerReference,
            ownerReference));
  }

}
