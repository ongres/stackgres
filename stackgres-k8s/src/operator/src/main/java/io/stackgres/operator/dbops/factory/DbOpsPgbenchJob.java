/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsBenchmark;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbench;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.PatroniSecret;
import io.stackgres.operator.patroni.factory.PatroniServices;

@ApplicationScoped
public class DbOpsPgbenchJob extends DbOpsJob {

  @Inject
  public DbOpsPgbenchJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ObjectMapperProvider objectMapperProvider,
      LabelFactory<StackGresCluster> labelFactory) {
    super(clusterPodSecurityContext, clusterStatefulSetEnvironmentVariables,
        objectMapperProvider.objectMapper(), labelFactory);
  }

  public DbOpsPgbenchJob() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected String operation() {
    return "pgbench";
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context, StackGresDbOps dbOps) {
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
        .toPlainString();
    final String duration = String.valueOf(Duration.parse(pgbench.getDuration()).getSeconds());
    List<EnvVar> runEnvVars = ImmutableList.of(
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
        .build());
    return runEnvVars;
  }

  @Override
  protected String setResultScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH
        .filename();
  }

  @Override
  protected String setResultStriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_PGBENCH_RESULT_SH_PATH.path();
  }

  @Override
  protected String runScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.filename();
  }

  @Override
  protected String runScriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_PGBENCH_SH_PATH.path();
  }

}
