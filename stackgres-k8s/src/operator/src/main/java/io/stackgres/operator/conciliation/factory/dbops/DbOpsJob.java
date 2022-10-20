/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

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
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpecScheduling;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class DbOpsJob implements JobFactory {

  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("(\\p{javaUpperCase})");

  private final ResourceFactory<StackGresDbOpsContext, PodSecurityContext> podSecurityFactory;
  private final DbOpsEnvironmentVariables clusterEnvironmentVariables;
  private final Map<DbOpsStatusCondition, String> conditions;
  protected final LabelFactoryForCluster<StackGresCluster> labelFactory;
  protected final LabelFactoryForDbOps dbOpsLabelFactory;
  protected final KubectlUtil kubectl;
  private final DbOpsVolumeMounts dbOpsVolumeMounts;
  private final DbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory;

  protected DbOpsJob(ResourceFactory<StackGresDbOpsContext, PodSecurityContext> podSecurityFactory,
      DbOpsEnvironmentVariables clusterEnvironmentVariables,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      LabelFactoryForDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      DbOpsVolumeMounts dbOpsVolumeMounts,
      DbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory) {
    this.podSecurityFactory = podSecurityFactory;
    this.clusterEnvironmentVariables = clusterEnvironmentVariables;
    this.labelFactory = labelFactory;
    this.dbOpsLabelFactory = dbOpsLabelFactory;
    this.conditions = Optional.ofNullable(jsonMapper)
        .map(om -> Seq.of(DbOpsStatusCondition.values())
            .map(c -> Tuple.tuple(c, c.getCondition()))
            .peek(t -> t.v2.setLastTransitionTime("$LAST_TRANSITION_TIME"))
            .map(t -> t.map2(Unchecked.function(jsonMapper::writeValueAsString)))
            .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2)))
        .orElse(ImmutableMap.of());
    this.kubectl = kubectl;
    this.dbOpsVolumeMounts = dbOpsVolumeMounts;
    this.dbOpsTemplatesVolumeFactory = dbOpsTemplatesVolumeFactory;
  }

  public String jobName(StackGresDbOps dbOps) {
    return DbOpsUtil.jobName(dbOps);
  }

  protected String getOperation(StackGresDbOps dbOps) {
    return dbOps.getSpec().getOp();
  }

  protected boolean isExclusiveOp() {
    return false;
  }

  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context) {
    return List.of();
  }

  protected List<EnvVar> getSetResultEnvVars(StackGresDbOpsContext context) {
    return List.of();
  }

  protected String getRunImage(StackGresDbOpsContext context) {
    return StackGresUtil.getPatroniImageName(context.getCluster());
  }

  protected abstract ClusterStatefulSetPath getRunScript();

  protected String getSetResultImage(StackGresDbOpsContext context) {
    return kubectl.getImageName(context.getCluster());
  }

  protected ClusterStatefulSetPath getSetResultScript() {
    return null;
  }

  @Override
  public Job createJob(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getSource();
    final String retries = String.valueOf(DbOpsUtil.getCurrentRetry(dbOps));
    List<EnvVar> runEnvVars = getRunEnvVars(context);
    List<EnvVar> setResultEnvVars = getSetResultEnvVars(context);
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    final Map<String, String> labels = dbOpsLabelFactory.dbOpsPodLabels(context.getSource());
    final String timeout = DbOpsUtil.getTimeout(dbOps);
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(jobName(dbOps))
        .withLabels(labels)
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
        .withServiceAccountName(DbOpsRole.roleName(context))
        .withSecurityContext(podSecurityFactory.createResource(context))
        .withRestartPolicy("Never")
        .withNodeSelector(Optional.ofNullable(dbOps)
            .map(StackGresDbOps::getSpec)
            .map(StackGresDbOpsSpec::getScheduling)
            .map(StackGresDbOpsSpecScheduling::getNodeSelector)
            .orElse(null))
        .withTolerations(Optional.ofNullable(dbOps)
            .map(StackGresDbOps::getSpec)
            .map(StackGresDbOpsSpec::getScheduling)
            .map(StackGresDbOpsSpecScheduling::getTolerations)
            .map(tolerations -> Seq.seq(tolerations)
                .map(TolerationBuilder::new)
                .map(TolerationBuilder::build)
                .toList())
            .orElse(null))
        .withAffinity(new AffinityBuilder()
            .withNodeAffinity(Optional.of(dbOps)
                .map(StackGresDbOps::getSpec)
                .map(StackGresDbOpsSpec::getScheduling)
                .map(StackGresDbOpsSpecScheduling::getNodeAffinity)
                .orElse(null))
            .withPodAffinity(Optional.of(dbOps)
                .map(StackGresDbOps::getSpec)
                .map(StackGresDbOpsSpec::getScheduling)
                .map(StackGresDbOpsSpecScheduling::getPodAffinity)
                .orElse(null))
            .withPodAntiAffinity(Optional.of(dbOps)
                .map(StackGresDbOps::getSpec)
                .map(StackGresDbOpsSpec::getScheduling)
                .map(StackGresDbOpsSpecScheduling::getPodAntiAffinity)
                .orElse(null))
            .build())
        .withInitContainers(new ContainerBuilder()
            .withName("set-dbops-running")
            .withImage(getSetResultImage(context))
            .withImagePullPolicy("IfNotPresent")
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
                        .withName("DB_OPS_NAME")
                        .withValue(name)
                        .build(),
                    new EnvVarBuilder()
                        .withName("DB_OPS_CRD_NAME")
                        .withValue(CustomResource.getCRDName(StackGresDbOps.class))
                        .build(),
                    new EnvVarBuilder()
                        .withName("CURRENT_RETRY")
                        .withValue(retries)
                        .build(),
                    new EnvVarBuilder()
                        .withName("HOME")
                        .withValue("/tmp")
                        .build())
                .addAll(Seq.of(DbOpsStatusCondition.values())
                    .map(c -> new EnvVarBuilder()
                        .withName("CONDITION_" + c.name())
                        .withValue(conditions.get(c))
                        .build())
                    .toList())
                .build())
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RUNNING_SH_PATH.path())
            .withVolumeMounts(dbOpsVolumeMounts.getVolumeMounts(context))
            .build())
        .withContainers(
            new ContainerBuilder()
                .withName("run-dbops")
                .withImage(getRunImage(context))
                .withImagePullPolicy("IfNotPresent")
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
                                .map(ClusterStatefulSetPath::path)
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
                    ClusterStatefulSetPath.LOCAL_BIN_RUN_DBOPS_SH_PATH.path())
                .withVolumeMounts(dbOpsVolumeMounts.getVolumeMounts(context))
                .build(),
            new ContainerBuilder()
                .withName("set-dbops-result")
                .withImage(kubectl.getImageName(context.getCluster()))
                .withImagePullPolicy("IfNotPresent")
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
                                .map(ClusterStatefulSetPath::path)
                                .orElse(""))
                            .build(),
                        new EnvVarBuilder()
                            .withName("CLUSTER_NAMESPACE")
                            .withValue(namespace)
                            .build(),
                        new EnvVarBuilder()
                            .withName("DB_OPS_NAME")
                            .withValue(name)
                            .build(),
                        new EnvVarBuilder()
                            .withName("DB_OPS_CRD_NAME")
                            .withValue(CustomResource.getCRDName(StackGresDbOps.class))
                            .build(),
                        new EnvVarBuilder()
                            .withName("JOB_POD_LABELS")
                            .withValue(Seq.seq(labels)
                                .append(Tuple.tuple("job-name", jobName(dbOps)))
                                .map(t -> t.v1 + "=" + t.v2).toString(","))
                            .build(),
                        new EnvVarBuilder()
                            .withName("HOME")
                            .withValue("/tmp")
                            .build())
                    .addAll(Seq.of(DbOpsStatusCondition.values())
                        .map(c -> new EnvVarBuilder()
                            .withName("CONDITION_" + c.name())
                            .withValue(conditions.get(c))
                            .build())
                        .toList())
                    .addAll(setResultEnvVars)
                    .build())
                .withCommand("/bin/sh", "-ex",
                    ClusterStatefulSetPath.LOCAL_BIN_SET_DBOPS_RESULT_SH_PATH.path())
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
