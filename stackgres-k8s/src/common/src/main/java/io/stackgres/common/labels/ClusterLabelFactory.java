/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.labels;

import static io.stackgres.operatorframework.resource.ResourceUtil.labelValue;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ClusterLabelFactory
    extends AbstractLabelFactory<StackGresCluster>
    implements LabelFactoryForCluster {

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
  public Map<String, String> defaultConfigLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().defaultConfigKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUid(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> clusterLabelsWithoutUid(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)))
        .put(labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> patroniClusterLabels(StackGresCluster resource) {
    return Map.of(labelMapper().appKey(), labelMapper().appName(),
        labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)),
        labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE);
  }

  @Override
  public Map<String, String> clusterPrimaryLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, getPrimaryRole(resource))
        .build();
  }

  @Override
  public Map<String, String> clusterLabelsWithoutUidAndScope(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().clusterKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterPrimaryLabelsWithoutUidAndScope(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabelsWithoutUidAndScope(resource))
        .put(PatroniUtil.ROLE_KEY, getPrimaryRole(resource))
        .build();
  }

  @Override
  public Map<String, String> clusterReplicaLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(PatroniUtil.ROLE_KEY, PatroniUtil.REPLICA_ROLE)
        .put(PatroniUtil.NOLOADBALANCE_TAG, PatroniUtil.FALSE_TAG_VALUE)
        .build();
  }

  @Override
  public Map<String, String> statefulSetPodLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(clusterLabels(resource))
        .put(labelMapper().disruptableKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> scheduledBackupPodLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().scheduledBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public Map<String, String> clusterCrossNamespaceLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceNamespaceKey(resource), labelValue(resourceNamespace(resource)))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .build();
  }

  @Override
  public Map<String, String> replicationInitializationBackupLabels(StackGresCluster resource) {
    return ImmutableMap.<String, String>builder().putAll(genericLabels(resource))
        .put(labelMapper().resourceScopeKey(resource), labelValue(resourceScope(resource)))
        .put(labelMapper().resourceUidKey(resource), labelValue(resourceUid(resource)))
        .put(labelMapper().replicationInitializationBackupKey(resource), StackGresContext.RIGHT_VALUE)
        .build();
  }

  @Override
  public String resourceScope(@NotNull StackGresCluster resource) {
    return Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getPatroni)
        .map(patroni -> patroni.getInitialConfig())
        .map(patroniConfig -> patroniConfig.getScope())
        .orElse(resourceName(resource));
  }

  private String getPrimaryRole(StackGresCluster resource) {
    final String patroniVersion = StackGresUtil.getPatroniVersion(resource);
    final int patroniMajorVersion = StackGresUtil.getPatroniMajorVersion(patroniVersion);
    if (patroniMajorVersion < PatroniUtil.PATRONI_VERSION_4) {
      return PatroniUtil.OLD_PRIMARY_ROLE;
    }
    return PatroniUtil.PRIMARY_ROLE;
  }

}
