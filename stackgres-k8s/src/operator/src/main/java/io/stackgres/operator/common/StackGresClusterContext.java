/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.configuration.OperatorContext;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.jooq.lambda.tuple.Tuple2;

public abstract class StackGresClusterContext implements ResourceHandlerContext {

  public abstract OperatorContext getOperatorContext();

  public abstract StackGresCluster getCluster();

  public abstract Optional<StackGresPostgresConfig> getPostgresConfig();

  public abstract Optional<StackGresBackupContext> getBackupContext();

  public abstract Optional<StackGresRestoreContext> getRestoreContext();

  public abstract Optional<StackGresProfile> getProfile();

  public abstract ImmutableList<SidecarEntry<?>> getSidecars();

  public abstract ImmutableList<StackGresBackup> getBackups();

  public abstract Optional<Prometheus> getPrometheus();

  @Override
  public abstract ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getExistingResources();

  @Override
  public abstract ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> getRequiredResources();

  public abstract String getClusterNamespace();

  public abstract String getClusterKey();

  public abstract String getClusterName();

  public abstract String getBackupKey();

  @Override
  public abstract ImmutableMap<String, String> getLabels();

  public abstract ImmutableList<OwnerReference> getOwnerReferences();

  public boolean isClusterPod(HasMetadata resource) {
    return resource instanceof Pod
        && resource.getMetadata().getNamespace().equals(getClusterNamespace())
        && Objects.equals(resource.getMetadata().getLabels().get(getClusterKey()),
        StackGresContext.RIGHT_VALUE)
        && resource.getMetadata().getName().matches(
        ResourceUtil.getNameWithIndexPattern(getClusterName()));
  }

  public boolean isBackupPod(HasMetadata resource) {
    return resource instanceof Pod
        && resource.getMetadata().getNamespace().equals(getClusterNamespace())
        && Objects.equals(resource.getMetadata().getLabels().get(getBackupKey()),
        StackGresContext.RIGHT_VALUE);
  }

  /**
   * Return a sidecar config if present.
   */
  @SuppressWarnings("unchecked")
  public <C, S extends StackGresClusterSidecarResourceFactory<C>>
      Optional<C> getSidecarConfig(S sidecar) {
    for (SidecarEntry<?> entry : getSidecars()) {
      if (entry.getSidecar() == sidecar) {
        return entry.getConfig().map(config -> (C) config);
      }
    }
    throw new IllegalStateException("Sidecar " + sidecar.getClass()
        + " not found in cluster configuration");
  }

  public Map<String, String> clusterAnnotations() {
    return Optional.ofNullable(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getMetadata)
        .map(StackGresClusterPodMetadata::getAnnotations)
        .orElse(ImmutableMap.of());
  }

  public Map<String, String> posCustomLabels() {
    return Optional.ofNullable(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPod)
        .map(StackGresClusterPod::getMetadata)
        .map(StackGresClusterPodMetadata::getLabels)
        .orElse(ImmutableMap.of());
  }

}
