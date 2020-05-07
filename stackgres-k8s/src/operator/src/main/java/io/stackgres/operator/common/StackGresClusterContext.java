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
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPod;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodMetadata;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.immutables.value.Value.Derived;
import org.jooq.lambda.tuple.Tuple2;

public abstract class StackGresClusterContext implements ResourceHandlerContext {

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

  @Override
  public ImmutableMap<String, String> getLabels() {
    return clusterLabels();
  }

  abstract ClusterLabelMapper<?> clusterLabelMapper();

  public String appKey() {
    return clusterLabelMapper().appKey();
  }

  public String appName() {
    return clusterLabelMapper().appName();
  }

  public String clusterNameKey() {
    return clusterLabelMapper().clusterNameKey();
  }

  public String clusterNamespaceKey() {
    return clusterLabelMapper().clusterNamespaceKey();
  }

  public String clusterUidKey() {
    return clusterLabelMapper().clusterUidKey();
  }

  public String clusterScopeKey() {
    return clusterLabelMapper().clusterScopeKey();
  }

  public String clusterKey() {
    return clusterLabelMapper().clusterKey();
  }

  public String disruptibleKey() {
    return clusterLabelMapper().disruptibleKey();
  }

  public String backupKey() {
    return clusterLabelMapper().backupKey();
  }

  @Derived
  public String clusterName() {
    return clusterLabelMapper().clusterName();
  }

  @Derived
  public String clusterNamespace() {
    return clusterLabelMapper().clusterNamespace();
  }

  @Derived
  public String clusterUid() {
    return clusterLabelMapper().clusterUid();
  }

  @Derived
  public String clusterScope() {
    return clusterLabelMapper().clusterScope();
  }

  @Derived
  public ImmutableMap<String, String> defaultLabels() {
    return clusterLabelMapper().defaultLabels();
  }

  @Derived
  public ImmutableMap<String, String> genericClusterLabels() {
    return clusterLabelMapper().genericClusterLabels();
  }

  @Derived
  public ImmutableMap<String, String> clusterLabels() {
    return clusterLabelMapper().clusterLabels();
  }

  @Derived
  public ImmutableMap<String, String> clusterCrossNamespaceLabels() {
    return clusterLabelMapper().clusterCrossNamespaceLabels();
  }

  @Derived
  public ImmutableMap<String, String> anyPatroniClusterLabels() {
    return clusterLabelMapper().anyPatroniClusterLabels();
  }

  @Derived
  public ImmutableMap<String, String> patroniClusterLabels() {
    return clusterLabelMapper().patroniClusterLabels();
  }

  @Derived
  public ImmutableMap<String, String> patroniPrimaryLabels() {
    return clusterLabelMapper().patroniPrimaryLabels();
  }

  @Derived
  public ImmutableMap<String, String> patroniReplicaLabels() {
    return clusterLabelMapper().patroniReplicaLabels();
  }

  @Derived
  public ImmutableMap<String, String> statefulSetPodLabels() {
    return clusterLabelMapper().statefulSetPodLabels();
  }

  @Derived
  public ImmutableMap<String, String> backupPodLabels() {
    return clusterLabelMapper().backupPodLabels();
  }

  @Derived
  public ImmutableList<OwnerReference> ownerReferences() {
    return clusterLabelMapper().ownerReferences();
  }

  public boolean isClusterPod(HasMetadata resource) {
    return clusterLabelMapper().isClusterPod(resource);
  }

  public boolean isBackupPod(HasMetadata resource) {
    return clusterLabelMapper().isBackupPod(resource);
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

  abstract static class ClusterLabelMapper<T extends HasMetadata> extends LabelMapper {
    abstract T clusterResource();

    public String clusterName() {
      return clusterResource().getMetadata().getName();
    }

    public String clusterNamespace() {
      return clusterResource().getMetadata().getNamespace();
    }

    public String clusterUid() {
      return clusterResource().getMetadata().getUid();
    }

    public String clusterScope() {
      return ResourceUtil.labelValue(clusterName());
    }

    public ImmutableMap<String, String> genericClusterLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()));
    }

    public ImmutableMap<String, String> clusterLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()));
    }

    public ImmutableMap<String, String> clusterCrossNamespaceLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterNamespaceKey(), ResourceUtil.labelValue(clusterNamespace()),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()));
    }

    public ImmutableMap<String, String> patroniClusterLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()),
          clusterKey(), StackGresUtil.RIGHT_VALUE);
    }

    public ImmutableMap<String, String> patroniPrimaryLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()),
          clusterKey(), StackGresUtil.RIGHT_VALUE,
          StackGresUtil.ROLE_KEY, StackGresUtil.PRIMARY_ROLE);
    }

    public ImmutableMap<String, String> patroniReplicaLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()),
          clusterKey(), StackGresUtil.RIGHT_VALUE,
          StackGresUtil.ROLE_KEY, StackGresUtil.REPLICA_ROLE);
    }

    public ImmutableMap<String, String> statefulSetPodLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()),
          clusterKey(), StackGresUtil.RIGHT_VALUE,
          disruptibleKey(), StackGresUtil.RIGHT_VALUE);
    }

    public ImmutableMap<String, String> backupPodLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterUidKey(), ResourceUtil.labelValue(clusterUid()),
          clusterNameKey(), ResourceUtil.labelValue(clusterName()),
          backupKey(), StackGresUtil.RIGHT_VALUE);
    }

    public ImmutableList<OwnerReference> ownerReferences() {
      return ImmutableList.of(ResourceUtil.getOwnerReference(clusterResource()));
    }

    public boolean isClusterPod(HasMetadata resource) {
      return resource instanceof Pod
          && resource.getMetadata().getNamespace().equals(clusterNamespace())
          && Objects.equals(resource.getMetadata().getLabels().get(clusterKey()),
              StackGresUtil.RIGHT_VALUE)
          && resource.getMetadata().getName().matches(
              ResourceUtil.getNameWithIndexPattern(clusterName()));
    }

    public boolean isBackupPod(HasMetadata resource) {
      return resource instanceof Pod
          && resource.getMetadata().getNamespace().equals(clusterNamespace())
          && Objects.equals(resource.getMetadata().getLabels().get(backupKey()),
              StackGresUtil.RIGHT_VALUE);
    }
  }

  abstract static class LabelMapper {
    public String appKey() {
      return StackGresUtil.APP_KEY;
    }

    public abstract String appName();

    public abstract String clusterNameKey();

    public abstract String clusterNamespaceKey();

    public abstract String clusterUidKey();

    public String clusterScopeKey() {
      return ResourceUtil.labelKey(clusterNameKey());
    }

    public String clusterKey() {
      return StackGresUtil.CLUSTER_KEY;
    }

    public String disruptibleKey() {
      return StackGresUtil.DISRUPTIBLE_KEY;
    }

    public String backupKey() {
      return StackGresUtil.BACKUP_KEY;
    }

    public ImmutableMap<String, String> defaultLabels() {
      return ImmutableMap.of(appKey(), appName());
    }

    public ImmutableMap<String, String> anyPatroniClusterLabels() {
      return ImmutableMap.of(appKey(), appName(),
          clusterKey(), StackGresUtil.RIGHT_VALUE);
    }
  }
}
