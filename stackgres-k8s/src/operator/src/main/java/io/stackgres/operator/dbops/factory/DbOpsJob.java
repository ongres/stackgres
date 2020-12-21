/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsDefinition;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.sidecars.pgutils.PostgresUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbOpsJob
    implements SubResourceStreamFactory<HasMetadata, StackGresDbOpsContext> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsJob.class);

  private final StackGresPodSecurityContext clusterPodSecurityContext;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final LabelFactory<StackGresCluster> labelFactory;
  private final ImmutableMap<DbOpsStatusCondition, String> conditions;

  @Inject
  public DbOpsJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ObjectMapper objectMapper, LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterPodSecurityContext = clusterPodSecurityContext;
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.labelFactory = labelFactory;
    this.conditions = Optional.ofNullable(objectMapper)
        .map(om -> Seq.of(DbOpsStatusCondition.values())
            .map(c -> Tuple.tuple(c, c.getCondition()))
            .peek(t -> t.v2.setLastTransitionTime("$LAST_TRANSITION_TIME"))
            .map(t -> t.map2(Unchecked.function(objectMapper::writeValueAsString)))
            .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)))
        .orElse(ImmutableMap.of());
  }

  public String jobName(StackGresDbOps dbOps) {
    String name = dbOps.getMetadata().getName();
    UUID uid = UUID.fromString(dbOps.getMetadata().getUid());
    return ResourceUtil.resourceName(name + "-" + operation() + "-"
        + Long.toHexString(uid.getMostSignificantBits())
        + "-" + getCurrentRetry(dbOps));
  }

  protected abstract String operation();

  private Integer getCurrentRetry(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .map(r -> r + (DbOps.isFailed(dbOps) && !DbOps.isMaxRetriesReached(dbOps) ? 1 : 0))
        .orElse(0);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getCurrentDbOps();
    return Seq.of(createJob(context, dbOps));
  }

  private Job createJob(StackGresDbOpsContext context, StackGresDbOps dbOps) {
    List<EnvVar> runEnvVars = getRunEnvVars(context, dbOps);
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    final Map<String, String> labels = labelFactory.dbOpsPodLabels(context.getCluster());
    final String timeout = Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(Duration::getSeconds)
        .map(Object::toString)
        .orElseGet(() -> String.valueOf(Integer.MAX_VALUE));
    final String pgVersion = context.getCluster().getSpec().getPostgresVersion();
    final String retries = String.valueOf(getCurrentRetry(dbOps));
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(dbOps))
        .withLabels(labels)
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(0)
        .withCompletions(1)
        .withParallelism(1)
        .withNewTemplate()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(dbOps))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withSecurityContext(clusterPodSecurityContext.createResource(context))
        .withRestartPolicy("Never")
        .withServiceAccountName(DbOpsRole.roleName(context))
        .withInitContainers(new ContainerBuilder()
            .withName("set-dbops-running")
            .withImage(StackGresContext.KUBECTL_IMAGE)
            .withImagePullPolicy("IfNotPresent")
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
                .add(new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(namespace)
                    .build(),
                    new EnvVarBuilder()
                    .withName("DB_OPS_NAME")
                    .withValue(name)
                    .build(),
                    new EnvVarBuilder()
                    .withName("DB_OPS_CRD_NAME")
                    .withValue(StackGresDbOpsDefinition.NAME)
                    .build(),
                    new EnvVarBuilder()
                    .withName("CURRENT_RETRY")
                    .withValue(retries)
                    .build())
                .addAll(Seq.of(DbOpsStatusCondition.values())
                    .map(c -> new EnvVarBuilder()
                        .withName("CONDITION_" + c.name())
                        .withValue(conditions.get(c))
                        .build())
                    .toList())
                .build())
            .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH.path())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH
                    .filename())
                .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH.path())
                .withReadOnly(true)))
            .build())
        .withContainers(
            new ContainerBuilder()
            .withName("run-dbops")
            .withImage(String.format(PostgresUtil.IMAGE_NAME, pgVersion,
                StackGresProperty.CONTAINER_BUILD.getString()))
            .withImagePullPolicy("IfNotPresent")
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
                .add(new EnvVarBuilder()
                    .withName("TIMEOUT")
                    .withValue(timeout)
                    .build())
                .addAll(runEnvVars)
                .build())
            .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                runScriptPath())
            .withVolumeMounts(
                ClusterStatefulSetVolumeConfig.SHARED.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                    volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(runScriptFilename())
                    .withMountPath(runScriptPath())
                    .withReadOnly(true)))
            .build(),
            new ContainerBuilder()
            .withName("set-dbops-result")
            .withImage(StackGresContext.KUBECTL_IMAGE)
            .withImagePullPolicy("IfNotPresent")
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
                .add(new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(namespace)
                    .build(),
                    new EnvVarBuilder()
                    .withName("DB_OPS_NAME")
                    .withValue(name)
                    .build(),
                    new EnvVarBuilder()
                    .withName("DB_OPS_CRD_NAME")
                    .withValue(StackGresDbOpsDefinition.NAME)
                    .build(),
                    new EnvVarBuilder()
                    .withName("JOB_POD_LABELS")
                    .withValue(Seq.seq(labels)
                        .append(Tuple.tuple("job-name", jobName(dbOps)))
                        .map(t -> t.v1 + "=" + t.v2).toString(","))
                    .build())
                .addAll(Seq.of(DbOpsStatusCondition.values())
                    .map(c -> new EnvVarBuilder()
                        .withName("CONDITION_" + c.name())
                        .withValue(conditions.get(c))
                        .build())
                    .toList())
                .build())
            .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                setResultStriptPath())
            .withVolumeMounts(
                ClusterStatefulSetVolumeConfig.SHARED.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                    volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(setResultScriptFilename())
                    .withMountPath(setResultStriptPath())
                    .withReadOnly(true)))
            .build())
        .withVolumes(
            ClusterStatefulSetVolumeConfig.SHARED.volume(context),
            new VolumeBuilder(ClusterStatefulSetVolumeConfig.TEMPLATES.volume(context))
            .editConfigMap()
            .withDefaultMode(0555) // NOPMD
            .endConfigMap()
            .build())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  protected abstract List<EnvVar> getRunEnvVars(StackGresDbOpsContext context,
      StackGresDbOps dbOps);

  protected abstract String setResultScriptFilename();

  protected abstract String setResultStriptPath();

  protected abstract String runScriptFilename();

  protected abstract String runScriptPath();

}
