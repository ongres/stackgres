/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabels;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.factory.PatroniScriptsConfigMap;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple4;

public abstract class StackGresClusterContext implements ResourceHandlerContext, ClusterContext {

  public abstract OperatorPropertyContext getOperatorContext();

  public abstract Optional<StackGresPostgresConfig> getPostgresConfig();

  public abstract Optional<StackGresBackupContext> getBackupContext();

  public abstract Optional<StackGresRestoreContext> getRestoreContext();

  public abstract Optional<StackGresProfile> getProfile();

  public abstract ImmutableList<SidecarEntry<?>> getSidecars();

  public abstract ImmutableList<StackGresBackup> getBackups();

  public abstract ImmutableList<StackGresDbOps> getDbOps();

  public abstract Optional<Prometheus> getPrometheus();

  public abstract ImmutableList<StackGresClusterScriptEntry> getInternalScripts();

  public abstract String getClusterNamespace();

  public abstract String getClusterKey();

  public abstract String getClusterName();

  public abstract String getScheduledBackupKey();

  public abstract String getBackupKey();

  public abstract String getDbOpsKey();

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
        && (Objects.equals(resource.getMetadata().getLabels().get(getScheduledBackupKey()),
            StackGresContext.RIGHT_VALUE)
            || Objects.equals(resource.getMetadata().getLabels().get(getBackupKey()),
                StackGresContext.RIGHT_VALUE));
  }

  public boolean isDbOpsPod(HasMetadata resource) {
    return resource instanceof Pod
        && resource.getMetadata().getNamespace().equals(getClusterNamespace())
        && Objects.equals(resource.getMetadata().getLabels().get(getDbOpsKey()),
                StackGresContext.RIGHT_VALUE);
  }

  /**
   * Return a sidecar config if present.
   */
  @SuppressWarnings("unchecked")
  public <C, S extends StackGresClusterSidecarResourceFactory<C>>
      Optional<C> getSidecarConfig(@NotNull S sidecar) {
    for (SidecarEntry<?> entry : getSidecars()) {
      if (entry.getSidecar() == sidecar) {
        return entry.getConfig().map(config -> (C) config);
      }
    }
    throw new IllegalStateException("Sidecar " + sidecar.getClass()
        + " not found in cluster configuration");
  }

  public Map<String, String> podsCustomLabels() {
    return Optional.ofNullable(getCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getMetadata)
        .map(StackGresClusterSpecMetadata::getLabels)
        .map(StackGresClusterSpecLabels::getClusterPods)
        .orElse(Map.of());
  }

  public Seq<Tuple4<StackGresClusterScriptEntry, Long, String, Long>> getIndexedScripts() {
    return Seq.seq(getInternalScripts())
        .zipWithIndex()
        .map(t -> t.concat(PatroniScriptsConfigMap.INTERNAL_SCRIPT))
        .append(Seq.of(Optional.ofNullable(
            getCluster().getSpec().getInitData())
            .map(StackGresClusterInitData::getScripts))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(List::stream)
            .zipWithIndex()
            .map(t -> t.concat(PatroniScriptsConfigMap.SCRIPT)))
        .zipWithIndex()
        .map(t -> t.v1.concat(t.v2));
  }
}
