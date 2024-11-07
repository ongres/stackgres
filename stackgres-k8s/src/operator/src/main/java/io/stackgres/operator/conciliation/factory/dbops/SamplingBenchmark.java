/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.crd.sgdbops.DbOpsSamplingMode;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmarkCredentials;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSampling;
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
@BenchmarkJob("sampling")
public class SamplingBenchmark extends AbstractDbOpsJob {

  @Inject
  public SamplingBenchmark(
      ResourceFactory<StackGresDbOpsContext, PodSecurityContext> podSecurityFactory,
      DbOpsEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      LabelFactoryForCluster labelFactory,
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
    StackGresDbOpsSampling sampling = benchmark.getSampling();
    final String primaryServiceDns = PatroniServices.readWriteName(context);
    final String serviceDns;
    if (benchmark.isConnectionTypePrimaryService()) {
      serviceDns = primaryServiceDns;
    } else {
      serviceDns = PatroniServices.readOnlyName(context);
    }
    final String topQueriesCollectDuration = String.valueOf(
        Duration.parse(sampling.getTopQueriesCollectDuration()).getSeconds());
    final String samplingDuration = String.valueOf(
        Duration.parse(sampling.getSamplingDuration()).getSeconds());
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
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getMode)
            .orElse(DbOpsSamplingMode.TIME.toString()))
        .build(),
        new EnvVarBuilder()
        .withName("TOP_QUERIES_COLLECT_DURATION")
        .withValue(topQueriesCollectDuration)
        .build(),
        new EnvVarBuilder()
        .withName("SAMPLING_DURATION")
        .withValue(samplingDuration)
        .build(),
        new EnvVarBuilder()
        .withName("CUSTOM_TOP_QUERIES_QUERY")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getCustomTopQueriesQuery)
            .map(value -> value.replace("$", "$$"))
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("QUERIES")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getQueries)
            .map(Object::toString)
            .orElse("10"))
        .build(),
        new EnvVarBuilder()
        .withName("TOP_QUERIES_FILTER")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getTopQueriesFilter)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("TOP_QUERIES_MIN")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getTopQueriesMin)
            .map(Object::toString)
            .orElse("5"))
        .build(),
        new EnvVarBuilder()
        .withName("TOP_QUERIES_PERCENTILE")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getTopQueriesPercentile)
            .map(Object::toString)
            .orElse("95"))
        .build(),
        new EnvVarBuilder()
        .withName("SAMPLING_MIN_INTERVAL")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getSamplingMinInterval)
            .map(Object::toString)
            .orElse("10000"))
        .build(),
        new EnvVarBuilder()
        .withName("DATABASE")
        .withValue(Optional.of(benchmark)
            .map(StackGresDbOpsBenchmark::getDatabase)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("TARGET_DATABASE")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getTargetDatabase)
            .orElse("postgres"))
        .build(),
        new EnvVarBuilder()
        .withName("READ_WRITE")
        .withValue(Optional.of(benchmark)
            .map(StackGresDbOpsBenchmark::isConnectionTypePrimaryService)
            .map(String::valueOf)
            .orElse("true"))
        .build())
        .toList();
  }

  @Override
  protected List<EnvVar> getSetResultEnvVars(StackGresDbOpsContext context) {
    StackGresDbOps dbOps = context.getSource();
    StackGresDbOpsBenchmark benchmark = dbOps.getSpec().getBenchmark();
    StackGresDbOpsSampling sampling = benchmark.getSampling();
    return Seq.of(
        new EnvVarBuilder()
        .withName("OMIT_TOP_QUERIES_IN_STATUS")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getOmitTopQueriesInStatus)
            .map(Object::toString)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("QUERIES")
        .withValue(Optional.of(sampling)
            .map(StackGresDbOpsSampling::getQueries)
            .map(Object::toString)
            .orElse("10"))
        .build())
        .toList();
  }

  @Override
  protected ClusterPath getRunScript() {
    return ClusterPath.LOCAL_BIN_RUN_SAMPLING_SH_PATH;
  }

  @Override
  protected ClusterPath getSetResultScript() {
    return ClusterPath.LOCAL_BIN_SET_SAMPLING_RESULT_SH_PATH;
  }
}
