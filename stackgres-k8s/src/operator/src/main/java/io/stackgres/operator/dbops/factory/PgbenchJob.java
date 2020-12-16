/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.batch.Job;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsDefinition;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.PatroniSecret;
import io.stackgres.operator.patroni.factory.PatroniServices;
import io.stackgres.operator.sidecars.pgutils.PostgresUtil;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PgbenchJob
    implements SubResourceStreamFactory<HasMetadata, StackGresDbOpsContext> {

  public static String pgbenchJobName(StackGresDbOps dbOps) {
    String name = dbOps.getMetadata().getName();
    UUID uid = UUID.fromString(dbOps.getMetadata().getUid());
    return ResourceUtil.resourceName(name + "-pgbench-"
        + Long.toHexString(uid.getMostSignificantBits())
        + "-" + getCurrentRetry(dbOps));
  }

  private static Integer getCurrentRetry(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getStatus)
        .map(StackGresDbOpsStatus::getOpRetries)
        .map(r -> r + (DbOps.isFailed(dbOps) && !DbOps.isMaxRetriesReached(dbOps) ? 1 : 0))
        .orElse(0);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PgbenchJob.class);

  private final StackGresPodSecurityContext clusterPodSecurityContext;
  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final LabelFactory<StackGresCluster> labelFactory;
  private final ImmutableMap<DbOpsStatusCondition, String> conditions;

  @Inject
  public PgbenchJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ObjectMapperProvider objectMapperProvider,
      LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.clusterPodSecurityContext = clusterPodSecurityContext;
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.labelFactory = labelFactory;
    ObjectMapper objectMapper = objectMapperProvider.objectMapper();
    this.conditions = Seq.of(DbOpsStatusCondition.values())
        .map(c -> Tuple.tuple(c, c.getCondition()))
        .peek(t -> t.v2.setLastTransitionTime("$LAST_TRANSITION_TIME"))
        .map(t -> t.map2(Unchecked.function(objectMapper::writeValueAsString)))
        .collect(ImmutableMap.toImmutableMap(Tuple2::v1, Tuple2::v2));
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getCurrentDbOps();
    return Seq.of(createPgbenchJob(context, dbOps,
        dbOps.getSpec().getBenchmark(),
        dbOps.getSpec().getBenchmark().getPgbench()));
  }

  private Job createPgbenchJob(StackGresDbOpsContext context, StackGresDbOps dbOps,
      StackGresDbOpsBenchmark benchmark, StackGresDbOpsPgbench pgbench) {
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    final Map<String, String> labels = labelFactory.dbOpsPodLabels(context.getCluster());
    final String timeout = Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getTimeout)
        .map(Duration::parse)
        .map(Object::toString)
        .orElseGet(() -> String.valueOf(Integer.MAX_VALUE));
    final String pgVersion = context.getCluster().getSpec().getPostgresVersion();
    final String primaryServiceDns = PatroniServices.readWriteName(context);
    final String serviceDns;
    if (benchmark.isConnectionTypePrimaryService()) {
      serviceDns = primaryServiceDns;
    } else {
      serviceDns = PatroniServices.readOnlyName(context);
    }
    final String scale = Quantity.getAmountInBytes(Quantity.parse(pgbench.getDatabaseSize()))
        .divide(Quantity.getAmountInBytes(Quantity.parse("16Mi")))
        .toPlainString();
    final String duration = String.valueOf(Duration.parse(pgbench.getDuration()).getSeconds());
    final String retries = String.valueOf(getCurrentRetry(dbOps));
    return new JobBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(pgbenchJobName(dbOps))
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
        .withName(pgbenchJobName(dbOps))
        .withLabels(labels)
        .endMetadata()
        .withNewSpec()
        .withSecurityContext(clusterPodSecurityContext.createResource(context))
        .withRestartPolicy("Never")
        .withServiceAccountName(DbOpsRole.roleName(context))
        .withContainers(new ContainerBuilder()
            .withName("pgbench-finished")
            .withImage(StackGresContext.KUBECTL_IMAGE)
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/true")
            .build())
        .withInitContainers(new ContainerBuilder()
            .withName("set-pgbench-running")
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
                ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RUNNING_SH_PATH.path())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RUNNING_SH_PATH
                    .filename())
                .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RUNNING_SH_PATH.path())
                .withReadOnly(true)))
            .build(),
            new ContainerBuilder()
            .withName("run-pgbench")
            .withImage(String.format(PostgresUtil.IMAGE_NAME, pgVersion,
                StackGresProperty.CONTAINER_BUILD.getString()))
            .withImagePullPolicy("IfNotPresent")
            .withEnv(ImmutableList.<EnvVar>builder()
                .addAll(clusterStatefulSetEnvironmentVariables.listResources(context))
                .add(new EnvVarBuilder()
                    .withName("TIMEOUT")
                    .withValue(timeout)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PGHOST")
                    .withValue(serviceDns)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PRIMARY_PGHOST")
                    .withValue(primaryServiceDns)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PGUSER")
                    .withValue("postgres")
                    .build(),
                    new EnvVarBuilder()
                    .withName("PGPASSWORD")
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName(PatroniSecret.name(context))
                    .withKey(PatroniSecret.SUPERUSER_PASSWORD_KEY)
                    .endSecretKeyRef()
                    .endValueFrom()
                    .build(),
                    new EnvVarBuilder()
                    .withName("SCALE")
                    .withValue(scale)
                    .build(),
                    new EnvVarBuilder()
                    .withName("DURATION")
                    .withValue(duration)
                    .build(),
                    new EnvVarBuilder()
                    .withName("PROTOCOL")
                    .withValue(Optional.of(pgbench)
                        .map(StackGresDbOpsPgbench::getUsePreparedStatements)
                        .map(usePreparedStatements -> usePreparedStatements ? "prepared" : "simple")
                        .orElse("simple"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("READ_WRITE")
                    .withValue(Optional.of(benchmark)
                        .map(StackGresDbOpsBenchmark::isConnectionTypePrimaryService)
                        .map(String::valueOf)
                        .orElse("true"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("CLIENTS")
                    .withValue(Optional.of(pgbench)
                        .map(StackGresDbOpsPgbench::getConcurrentClients)
                        .map(String::valueOf)
                        .orElse("1"))
                    .build(),
                    new EnvVarBuilder()
                    .withName("JOBS")
                    .withValue(Optional.of(pgbench)
                        .map(StackGresDbOpsPgbench::getThreads)
                        .map(String::valueOf)
                        .orElse("1"))
                    .build())
                .build())
            .withCommand("/bin/bash", "-e" + (LOGGER.isTraceEnabled() ? "x" : ""),
                ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.path())
            .withVolumeMounts(
                ClusterStatefulSetVolumeConfig.SHARED.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                    volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.filename())
                    .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.path())
                    .withReadOnly(true)))
            .build(),
            new ContainerBuilder()
            .withName("set-pgbench-result")
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
                        .append(Tuple.tuple("job-name", pgbenchJobName(dbOps)))
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
                ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH.path())
            .withVolumeMounts(
                ClusterStatefulSetVolumeConfig.SHARED.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context,
                    volumeMountBuilder -> volumeMountBuilder
                    .withSubPath(ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH
                        .filename())
                    .withMountPath(ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH
                        .path())
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

}
