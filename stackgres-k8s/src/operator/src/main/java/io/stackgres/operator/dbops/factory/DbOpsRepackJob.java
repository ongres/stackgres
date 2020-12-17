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
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.ObjectMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepack;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepackConfig;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.PatroniSecret;
import io.stackgres.operator.patroni.factory.PatroniServices;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOpsRepackJob extends DbOpsJob {

  @Inject
  public DbOpsRepackJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ObjectMapperProvider objectMapperProvider,
      LabelFactory<StackGresCluster> labelFactory) {
    super(clusterPodSecurityContext, clusterStatefulSetEnvironmentVariables,
        objectMapperProvider.objectMapper(), labelFactory);
  }

  public DbOpsRepackJob() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected String operation() {
    return "repack";
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context, StackGresDbOps dbOps) {
    StackGresDbOpsRepack repack = dbOps.getSpec().getRepack();
    final String primaryServiceDns = PatroniServices.readWriteName(context);
    List<EnvVar> runEnvVars = ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
            .withName("PGHOST")
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
            .build())
        .addAll(getRepackConfigEnvVar(repack))
        .add(new EnvVarBuilder()
            .withName("DATABASES")
            .withValue(Seq.seq(Optional.ofNullable(repack)
                .map(StackGresDbOpsRepack::getDatabases)
                .stream())
                .flatMap(List::stream)
                .map(database -> Seq.seq(getRepackConfigEnvVar(repack))
                    .map(envVar -> envVar.getName() + "=" + envVar.getValue())
                    .toString(";") + " " + database.getName())
                .toString("\n"))
            .build())
        .build();
    return runEnvVars;
  }

  private ImmutableList<EnvVar> getRepackConfigEnvVar(StackGresDbOpsRepackConfig repackConfig) {
    return ImmutableList.of(
        new EnvVarBuilder()
        .withName("NO_ORDER")
        .withValue(Optional.ofNullable(repackConfig)
            .map(StackGresDbOpsRepackConfig::getNoOrder)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("WAIT_TIMEOUT")
        .withValue(Optional.ofNullable(repackConfig)
            .map(StackGresDbOpsRepackConfig::getWaitTimeout)
            .map(Duration::parse)
            .map(Duration::getSeconds)
            .map(String::valueOf)
            .orElse(""))
        .build(),
        new EnvVarBuilder()
        .withName("NO_KILL_BACKEND")
        .withValue(Optional.ofNullable(repackConfig)
            .map(StackGresDbOpsRepackConfig::getNoKillBackend)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("NO_ANALYZE")
        .withValue(Optional.ofNullable(repackConfig)
            .map(StackGresDbOpsRepackConfig::getNoAnalyze)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("EXCLUDE_EXTENSION")
        .withValue(Optional.ofNullable(repackConfig)
            .map(StackGresDbOpsRepackConfig::getExcludeExtension)
            .map(String::valueOf)
            .orElse("false"))
        .build());
  }

  @Override
  protected String setResultScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_REPACK_RESULT_SH_PATH
        .filename();
  }

  @Override
  protected String setResultStriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_REPACK_RESULT_SH_PATH.path();
  }

  @Override
  protected String runScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_REPACK_SH_PATH.filename();
  }

  @Override
  protected String runScriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_REPACK_SH_PATH.path();
  }

}
