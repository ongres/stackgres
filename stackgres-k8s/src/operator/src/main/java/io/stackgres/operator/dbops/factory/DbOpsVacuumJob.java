/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.dbops.factory;

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
import io.stackgres.common.crd.sgdbops.StackGresDbOpsVacuum;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsVacuumConfig;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.common.StackGresDbOpsContext;
import io.stackgres.operator.common.StackGresPodSecurityContext;
import io.stackgres.operator.patroni.factory.PatroniSecret;
import io.stackgres.operator.patroni.factory.PatroniServices;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOpsVacuumJob extends DbOpsJob {

  @Inject
  public DbOpsVacuumJob(StackGresPodSecurityContext clusterPodSecurityContext,
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      ObjectMapperProvider objectMapperProvider,
      LabelFactory<StackGresCluster> labelFactory) {
    super(clusterPodSecurityContext, clusterStatefulSetEnvironmentVariables,
        objectMapperProvider.objectMapper(), labelFactory);
  }

  public DbOpsVacuumJob() {
    super(null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected String operation() {
    return "vacuum";
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context, StackGresDbOps dbOps) {
    StackGresDbOpsVacuum vacuum = dbOps.getSpec().getVacuum();
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
        .addAll(getVacuumConfigEnvVar(vacuum))
        .add(new EnvVarBuilder()
            .withName("DATABASES")
            .withValue(Seq.seq(Optional.ofNullable(vacuum)
                .map(StackGresDbOpsVacuum::getDatabases)
                .stream())
                .flatMap(List::stream)
                .map(database -> Seq.seq(getVacuumConfigEnvVar(vacuum))
                    .map(envVar -> envVar.getName() + "=" + envVar.getValue())
                    .toString(";") + " " + database.getName())
                .toString("\n"))
            .build())
        .build();
    return runEnvVars;
  }

  private ImmutableList<EnvVar> getVacuumConfigEnvVar(StackGresDbOpsVacuumConfig vacuumConfig) {
    return ImmutableList.of(
        new EnvVarBuilder()
        .withName("FULL")
        .withValue(Optional.ofNullable(vacuumConfig)
            .map(StackGresDbOpsVacuumConfig::getFull)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("FREEZE")
        .withValue(Optional.ofNullable(vacuumConfig)
            .map(StackGresDbOpsVacuumConfig::getFreeze)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("ANALYZE")
        .withValue(Optional.ofNullable(vacuumConfig)
            .map(StackGresDbOpsVacuumConfig::getAnalyze)
            .map(String::valueOf)
            .orElse("false"))
        .build(),
        new EnvVarBuilder()
        .withName("DISABLE_PAGE_SKIPPING")
        .withValue(Optional.ofNullable(vacuumConfig)
            .map(StackGresDbOpsVacuumConfig::getDisablePageSkipping)
            .map(String::valueOf)
            .orElse("false"))
        .build());
  }

  @Override
  protected String setResultScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_VACUUM_RESULT_SH_PATH
        .filename();
  }

  @Override
  protected String setResultStriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_SET_VACUUM_RESULT_SH_PATH.path();
  }

  @Override
  protected String runScriptFilename() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_VACUUM_SH_PATH.filename();
  }

  @Override
  protected String runScriptPath() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_VACUUM_SH_PATH.path();
  }

}
