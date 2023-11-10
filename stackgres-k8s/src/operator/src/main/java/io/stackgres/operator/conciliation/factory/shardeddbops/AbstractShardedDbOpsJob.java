/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsStatusCondition;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpecScheduling;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractShardedDbOpsJob implements ShardedDbOpsJobFactory {

  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("(\\p{javaUpperCase})");

  private final ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext>
      podSecurityFactory;
  private final ShardedDbOpsEnvironmentVariables clusterEnvironmentVariables;
  private final Map<ShardedDbOpsStatusCondition, String> conditions;
  protected final LabelFactoryForShardedDbOps dbOpsLabelFactory;
  protected final KubectlUtil kubectl;
  private final ShardedDbOpsVolumeMounts dbOpsVolumeMounts;
  private final ShardedDbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory;
  protected final ObjectMapper jsonMapper;

  protected AbstractShardedDbOpsJob(
      ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> podSecurityFactory,
      ShardedDbOpsEnvironmentVariables clusterEnvironmentVariables,
      LabelFactoryForShardedDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      ShardedDbOpsVolumeMounts dbOpsVolumeMounts,
      ShardedDbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory) {
    this.podSecurityFactory = podSecurityFactory;
    this.clusterEnvironmentVariables = clusterEnvironmentVariables;
    this.dbOpsLabelFactory = dbOpsLabelFactory;
    this.conditions = Optional.ofNullable(jsonMapper)
        .map(om -> Seq.of(ShardedDbOpsStatusCondition.values())
            .map(c -> Tuple.tuple(c, c.getCondition()))
            .peek(t -> t.v2.setLastTransitionTime("$LAST_TRANSITION_TIME"))
            .map(t -> t.map2(Unchecked.function(jsonMapper::writeValueAsString)))
            .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)))
        .orElse(ImmutableMap.of());
    this.kubectl = kubectl;
    this.dbOpsVolumeMounts = dbOpsVolumeMounts;
    this.dbOpsTemplatesVolumeFactory = dbOpsTemplatesVolumeFactory;
    this.jsonMapper = jsonMapper;
  }

  public AbstractShardedDbOpsJob() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.podSecurityFactory = null;
    this.clusterEnvironmentVariables = null;
    this.dbOpsLabelFactory = null;
    this.conditions = null;
    this.kubectl = null;
    this.dbOpsVolumeMounts = null;
    this.dbOpsTemplatesVolumeFactory = null;
    this.jsonMapper = null;
  }

  public String jobName(StackGresShardedDbOps dbOps) {
    return ShardedDbOpsUtil.jobName(dbOps);
  }

  protected String getOperation(StackGresShardedDbOps dbOps) {
    return dbOps.getSpec().getOp();
  }

  protected boolean isExclusiveOp() {
    return false;
  }

  protected List<EnvVar> getRunEnvVars(StackGresShardedDbOpsContext context) {
    return List.of();
  }

  protected List<EnvVar> getSetResultEnvVars(StackGresShardedDbOpsContext context) {
    return List.of();
  }

  protected String getRunImage(StackGresShardedDbOpsContext context) {
    return StackGresUtil.getPatroniImageName(context.getShardedCluster());
  }

  protected abstract ShardedClusterPath getRunScript();

  protected String getSetResultImage(StackGresShardedDbOpsContext context) {
    return kubectl.getImageName(context.getShardedCluster());
  }

  protected ShardedClusterPath getSetResultScript() {
    return null;
  }

  @Override
  public Job createJob(StackGresShardedDbOpsContext context) {
    final StackGresShardedDbOps dbOps = context.getSource();
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    final Map<String, String> labels =
        dbOpsLabelFactory.shardedDbOpsPodLabels(context.getSource());
    final Integer maxRetries = Optional.of(dbOps)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMaxRetries)
        .orElse(0);
    List<EnvVar> runEnvVars = getRunEnvVars(context);
    List<EnvVar> setResultEnvVars = getSetResultEnvVars(context);
    final String timeout = ShardedDbOpsUtil.getTimeout(dbOps);
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(dbOps))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withBackoffLimit(maxRetries)
        .withParallelism(1)
        .withNewTemplate()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(dbOps))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withServiceAccountName(ShardedDbOpsRole.roleName(context))
        .withSecurityContext(podSecurityFactory.createResource(context))
        .withRestartPolicy("Never")
        .withNodeSelector(Optional.ofNullable(dbOps)
            .map(StackGresShardedDbOps::getSpec)
            .map(StackGresShardedDbOpsSpec::getScheduling)
            .map(StackGresShardedDbOpsSpecScheduling::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(dbOps)
            .map(StackGresShardedDbOps::getSpec)
            .map(StackGresShardedDbOpsSpec::getScheduling)
            .map(StackGresShardedDbOpsSpecScheduling::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withAffinity(new AffinityBuilder()
            .withNodeAffinity(Optional.of(dbOps)
                .map(StackGresShardedDbOps::getSpec)
                .map(StackGresShardedDbOpsSpec::getScheduling)
                .map(StackGresShardedDbOpsSpecScheduling::getNodeAffinity)
                .orElse(null))
            .withPodAffinity(Optional.of(dbOps)
                .map(StackGresShardedDbOps::getSpec)
                .map(StackGresShardedDbOpsSpec::getScheduling)
                .map(StackGresShardedDbOpsSpecScheduling::getPodAffinity)
                .orElse(null))
            .withPodAntiAffinity(Optional.of(dbOps)
                .map(StackGresShardedDbOps::getSpec)
                .map(StackGresShardedDbOpsSpec::getScheduling)
                .map(StackGresShardedDbOpsSpecScheduling::getPodAntiAffinity)
                .orElse(null))
            .build())
        .withPriorityClassName(Optional.of(dbOps)
            .map(StackGresShardedDbOps::getSpec)
            .map(StackGresShardedDbOpsSpec::getScheduling)
            .map(StackGresShardedDbOpsSpecScheduling::getPriorityClassName)
            .orElse(null))
        .withInitContainers(new ContainerBuilder()
            .withName("set-dbops-running")
            .withImage(getSetResultImage(context))
            .withImagePullPolicy(getDefaultPullPolicy())
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(clusterEnvironmentVariables.listResources(context))
                .add(
                    new EnvVarBuilder()
                        .withName("OP_NAME")
                        .withValue(dbOps.getSpec().getOp())
                        .build(),
                    new EnvVarBuilder()
                        .withName("NORMALIZED_OP_NAME")
                        .withValue(UPPERCASE_PATTERN
                            .matcher(dbOps.getSpec().getOp())
                            .replaceAll(result -> " " + result.group(1).toLowerCase(Locale.US)))
                        .build(),
                    new EnvVarBuilder()
                        .withName("KEBAB_OP_NAME")
                        .withValue(UPPERCASE_PATTERN
                            .matcher(dbOps.getSpec().getOp())
                            .replaceAll(result -> "-" + result.group(1).toLowerCase(Locale.US)))
                        .build(),
                    new EnvVarBuilder()
                        .withName("CLUSTER_NAMESPACE")
                        .withValue(namespace)
                        .build(),
                    new EnvVarBuilder()
                        .withName("SHARDED_DBOPS_NAME")
                        .withValue(name)
                        .build(),
                    new EnvVarBuilder()
                        .withName("SHARDED_DBOPS_CRD_NAME")
                        .withValue(CustomResource.getCRDName(StackGresShardedDbOps.class))
                        .build(),
                    new EnvVarBuilder()
                        .withName("HOME")
                        .withValue("/tmp")
                        .build(),
                    new EnvVarBuilder()
                        .withName("LOCK_DURATION")
                        .withValue(OperatorProperty.LOCK_DURATION.getString())
                        .build(),
                    new EnvVarBuilder()
                        .withName("LOCK_POLL_INTERVAL")
                        .withValue(OperatorProperty.LOCK_POLL_INTERVAL.getString())
                        .build(),
                    new EnvVarBuilder()
                        .withName("LOCK_SERVICE_ACCOUNT_KEY")
                        .withValue(StackGresContext.LOCK_SERVICE_ACCOUNT_KEY)
                        .build(),
                    new EnvVarBuilder()
                        .withName("LOCK_POD_KEY")
                        .withValue(StackGresContext.LOCK_POD_KEY)
                        .build(),
                    new EnvVarBuilder()
                        .withName("LOCK_TIMEOUT_KEY")
                        .withValue(StackGresContext.LOCK_TIMEOUT_KEY)
                        .build())
                .addAll(Seq.of(ShardedDbOpsStatusCondition.values())
                    .map(c -> new EnvVarBuilder()
                        .withName("CONDITION_" + c.name())
                        .withValue(conditions.get(c))
                        .build())
                    .toList())
                .build())
            .withCommand("/bin/sh", "-ex",
                ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RUNNING_SH_PATH.path())
            .withVolumeMounts(dbOpsVolumeMounts.getVolumeMounts(context))
            .build())
        .withContainers(
            new ContainerBuilder()
                .withName("run-dbops")
                .withImage(getRunImage(context))
                .withImagePullPolicy(getDefaultPullPolicy())
                .withEnv(ImmutableList.<EnvVar>builder()
                    .addAll(clusterEnvironmentVariables
                        .listResources(context))
                    .add(
                        new EnvVarBuilder()
                            .withName("OP_NAME")
                            .withValue(dbOps.getSpec().getOp())
                            .build(),
                        new EnvVarBuilder()
                            .withName("EXCLUSIVE_OP")
                            .withValue(String.valueOf(isExclusiveOp()))
                            .build(),
                        new EnvVarBuilder()
                            .withName("NORMALIZED_OP_NAME")
                            .withValue(UPPERCASE_PATTERN
                                .matcher(dbOps.getSpec().getOp())
                                .replaceAll(result -> " " + result.group(1)
                                    .toLowerCase(Locale.US)))
                            .build(),
                        new EnvVarBuilder()
                            .withName("KEBAB_OP_NAME")
                            .withValue(UPPERCASE_PATTERN
                                .matcher(dbOps.getSpec().getOp())
                                .replaceAll(result -> "-" + result.group(1)
                                    .toLowerCase(Locale.US)))
                            .build(),
                        new EnvVarBuilder()
                            .withName("RUN_SCRIPT_PATH")
                            .withValue(Optional.ofNullable(getRunScript())
                                .map(ShardedClusterPath::path)
                                .orElse(""))
                            .build(),
                        new EnvVarBuilder()
                            .withName("TIMEOUT")
                            .withValue(timeout)
                            .build(),
                        new EnvVarBuilder()
                            .withName("HOME")
                            .withValue("/tmp")
                            .build())
                    .addAll(runEnvVars)
                    .build())
                .withCommand("/bin/sh", "-ex",
                    ShardedClusterPath.LOCAL_BIN_RUN_SHARDED_DBOPS_SH_PATH.path())
                .withVolumeMounts(dbOpsVolumeMounts.getVolumeMounts(context))
                .build(),
            new ContainerBuilder()
                .withName("set-dbops-result")
                .withImage(kubectl.getImageName(context.getShardedCluster()))
                .withImagePullPolicy(getDefaultPullPolicy())
                .withEnv(ImmutableList.<EnvVar>builder()
                    .addAll(clusterEnvironmentVariables
                        .listResources(context))
                    .add(
                        new EnvVarBuilder()
                            .withName("OP_NAME")
                            .withValue(dbOps.getSpec().getOp())
                            .build(),
                        new EnvVarBuilder()
                            .withName("NORMALIZED_OP_NAME")
                            .withValue(UPPERCASE_PATTERN
                                .matcher(dbOps.getSpec().getOp())
                                .replaceAll(result -> " "
                                    + result.group(1).toLowerCase(Locale.US)))
                            .build(),
                        new EnvVarBuilder()
                            .withName("KEBAB_OP_NAME")
                            .withValue(UPPERCASE_PATTERN
                                .matcher(dbOps.getSpec().getOp())
                                .replaceAll(result -> "-"
                                    + result.group(1).toLowerCase(Locale.US)))
                            .build(),
                        new EnvVarBuilder()
                            .withName("SET_RESULT_SCRIPT_PATH")
                            .withValue(Optional.ofNullable(getSetResultScript())
                                .map(ShardedClusterPath::path)
                                .orElse(""))
                            .build(),
                        new EnvVarBuilder()
                            .withName("CLUSTER_NAMESPACE")
                            .withValue(namespace)
                            .build(),
                        new EnvVarBuilder()
                            .withName("SHARDED_DBOPS_NAME")
                            .withValue(name)
                            .build(),
                        new EnvVarBuilder()
                            .withName("SHARDED_DBOPS_CRD_NAME")
                            .withValue(CustomResource.getCRDName(StackGresShardedDbOps.class))
                            .build(),
                        new EnvVarBuilder()
                            .withName("HOME")
                            .withValue("/tmp")
                            .build())
                    .addAll(Seq.of(ShardedDbOpsStatusCondition.values())
                        .map(c -> new EnvVarBuilder()
                            .withName("CONDITION_" + c.name())
                            .withValue(conditions.get(c))
                            .build())
                        .toList())
                    .addAll(setResultEnvVars)
                    .build())
                .withCommand("/bin/sh", "-ex",
                    ShardedClusterPath.LOCAL_BIN_SET_SHARDED_DBOPS_RESULT_SH_PATH.path())
                .withVolumeMounts(dbOpsVolumeMounts.getVolumeMounts(context))
                .build())
        .withVolumes(
            Seq.of(buildSharedVolume())
            .append(dbOpsTemplatesVolumeFactory.buildVolumes(context).map(VolumePair::getVolume))
            .toList())
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private Volume buildSharedVolume() {
    return new VolumeBuilder()
        .withName(StackGresVolume.SHARED.getName())
        .withEmptyDir(new EmptyDirVolumeSourceBuilder()
            .build())
        .build();
  }

}
