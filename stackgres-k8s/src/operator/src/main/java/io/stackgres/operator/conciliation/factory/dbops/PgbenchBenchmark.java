/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmarkCredentials;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchCustom;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchCustomScript;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSamplingStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniServices;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
@BenchmarkJob("pgbench")
public class PgbenchBenchmark extends AbstractDbOpsJob {

  @Inject
  public PgbenchBenchmark(
      ResourceFactory<StackGresDbOpsContext, PodSecurityContext> podSecurityFactory,
      DbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactoryForCluster<StackGresCluster> labelFactory,
      LabelFactoryForDbOps dbOpsLabelFactory,
      ObjectMapper jsonMapper,
      KubectlUtil kubectl,
      DbOpsVolumeMounts dbOpsVolumeMounts,
      DbOpsTemplatesVolumeFactory dbOpsTemplatesVolumeFactory) {
    super(podSecurityFactory, clusterStatefulSetEnvironmentVariables, labelFactory,
        dbOpsLabelFactory, jsonMapper, kubectl, dbOpsVolumeMounts, dbOpsTemplatesVolumeFactory);
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context) {
    StackGresDbOps dbOps = context.getSource();
    StackGresDbOpsBenchmark benchmark = dbOps.getSpec().getBenchmark();
    StackGresDbOpsPgbench pgbench = benchmark.getPgbench();
    final String primaryServiceDns = PatroniServices.readWriteName(context);
    final String serviceDns;
    if (benchmark.isConnectionTypePrimaryService()) {
      serviceDns = primaryServiceDns;
    } else {
      serviceDns = PatroniServices.readOnlyName(context);
    }
    final String scale = Quantity.getAmountInBytes(Quantity.parse(pgbench.getDatabaseSize()))
        .divide(Quantity.getAmountInBytes(Quantity.parse("16Mi")))
        .setScale(0, RoundingMode.UP)
        .toPlainString();
    final String duration = String.valueOf(Duration.parse(pgbench.getDuration()).getSeconds());
    final String samplingRate = Optional.ofNullable(pgbench.getSamplingRate())
        .map(new DecimalFormat("#.000000000")::format)
        .orElse("1.0");
    return Seq.of(
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
        .withNewValueFrom()
        .withSecretKeyRef(Optional.ofNullable(benchmark.getCredentials())
            .map(StackGresDbOpsBenchmarkCredentials::getUsername)
            .map(SecretKeySelector.class::cast)
            .orElseGet(() -> new SecretKeySelector(
                PatroniSecret.SUPERUSER_USERNAME_KEY,
                PatroniSecret.name(context.getCluster()),
                false)))
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName("PGPASSWORD")
        .withNewValueFrom()
        .withSecretKeyRef(Optional.ofNullable(benchmark.getCredentials())
            .map(StackGresDbOpsBenchmarkCredentials::getPassword)
            .map(SecretKeySelector.class::cast)
            .orElseGet(() -> new SecretKeySelector(
                PatroniSecret.SUPERUSER_PASSWORD_KEY,
                PatroniSecret.name(context.getCluster()),
                false)))
        .endValueFrom()
        .build(),
        new EnvVarBuilder()
        .withName("MODE")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getMode)
            .orElse(""))
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
        .withName("SAMPLING_RATE")
        .withValue(samplingRate)
        .build(),
        new EnvVarBuilder()
        .withName("FOREIGN_KEYS")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getForeignKeys)
            .map(Object::toString)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("UNLOGGED_TABLES")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getUnloggedTables)
            .map(Object::toString)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("PARTITION_METHOD")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getPartitionMethod)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("PARTITIONS")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getPartitions)
            .map(Object::toString)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("INIT_STEPS")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getInitSteps)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("FILLFACTOR")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getFillfactor)
            .map(Object::toString)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("NO_VACUUM")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getNoVacuum)
            .map(Object::toString)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("DATABASE")
        .withValue(Optional.of(benchmark)
            .map(StackGresDbOpsBenchmark::getDatabase)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("PROTOCOL")
        .withValue(Optional.of(pgbench)
            .map(StackGresDbOpsPgbench::getQueryMode)
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
        .append(
            Optional.ofNullable(pgbench.getCustom())
            .map(StackGresDbOpsPgbenchCustom::getInitialization)
            .map(script -> createEnvVarFromScript("INIT_SCRIPT", script)))
        .append(
            Seq.seq(Optional.ofNullable(pgbench.getCustom())
            .map(StackGresDbOpsPgbenchCustom::getScripts))
            .filter(Predicate.not(List::isEmpty))
            .map(scripts -> new EnvVarBuilder()
                .withName("SCRIPTS")
                .withValue(scripts.stream()
                    .map(script -> Optional.ofNullable(script.getBuiltin())
                        .or(() -> Optional.ofNullable(script.getReplay())
                            .map(replay -> "replay:" + replay))
                        .orElse("custom")
                        + " " + Optional.ofNullable(script.getWeight()).orElse(1))
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")))
                .build())
            .append(
                Seq.seq(Optional.ofNullable(pgbench.getCustom())
                    .map(StackGresDbOpsPgbenchCustom::getScripts))
                .flatMap(List::stream)
                .zipWithIndex()
                .map(script -> createEnvVarFromScript("SCRIPT_" + script.v2, script.v1))))
        .append(
            Seq.seq(context.getSamplingStatus()
            .map(StackGresDbOpsSamplingStatus::getQueries))
            .filter(Predicate.not(List::isEmpty))
            .map(queries -> new EnvVarBuilder()
                .withName("REPLAY_QUERIES")
                .withValue(String.valueOf(queries.size()))
                .build())
            .append(
                Seq.seq(context.getSamplingStatus()
                    .map(StackGresDbOpsSamplingStatus::getQueries))
                .flatMap(List::stream)
                .zipWithIndex()
                .map(query -> new EnvVarBuilder()
                    .withName("REPLAY_QUERY_" + query.v2)
                    .withValue(query.v1.getQuery())
                    .build())))
        .toList();
  }

  @Override
  protected List<EnvVar> getSetResultEnvVars(StackGresDbOpsContext context) {
    StackGresDbOps dbOps = context.getSource();
    StackGresDbOpsBenchmark benchmark = dbOps.getSpec().getBenchmark();
    StackGresDbOpsPgbench pgbench = benchmark.getPgbench();
    final String duration = String.valueOf(Duration.parse(pgbench.getDuration()).getSeconds());
    return Seq.of(
        new EnvVarBuilder()
        .withName("DURATION")
        .withValue(duration)
        .build())
        .append(
            Seq.seq(Optional.ofNullable(pgbench.getCustom())
            .map(StackGresDbOpsPgbenchCustom::getScripts))
            .filter(Predicate.not(List::isEmpty))
            .map(scripts -> new EnvVarBuilder()
                .withName("SCRIPTS")
                .withValue(scripts.stream()
                    .map(script -> Optional.ofNullable(script.getBuiltin()).orElse("custom")
                        + " " + Optional.ofNullable(script.getWeight()).orElse(1))
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")))
                .build()))
        .toList();
  }

  private EnvVar createEnvVarFromScript(String name, StackGresDbOpsPgbenchCustomScript script) {
    if (script.getScriptFrom() != null
        && script.getScriptFrom().getConfigMapKeyRef() != null) {
      return new EnvVarBuilder()
          .withName(name)
          .withNewValueFrom()
          .withConfigMapKeyRef(script.getScriptFrom().getConfigMapKeyRef())
          .endValueFrom()
          .build();
    }
    if (script.getScriptFrom() != null
        && script.getScriptFrom().getSecretKeyRef() != null) {
      return new EnvVarBuilder()
          .withName(name)
          .withNewValueFrom()
          .withSecretKeyRef(script.getScriptFrom().getSecretKeyRef())
          .endValueFrom()
          .build();
    }
    return new EnvVarBuilder()
        .withName(name)
        .withValue(Optional.of(script)
            .map(StackGresDbOpsPgbenchCustomScript::getScript)
            .map(value -> value.replace("$", "$$"))
            .orElse(""))
        .build();
  }

  @Override
  protected ClusterPath getRunScript() {
    return ClusterPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH;
  }

  @Override
  protected ClusterPath getSetResultScript() {
    return ClusterPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH;
  }
}
