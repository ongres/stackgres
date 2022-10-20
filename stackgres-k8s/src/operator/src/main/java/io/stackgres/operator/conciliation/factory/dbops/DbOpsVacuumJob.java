/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsVacuum;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsVacuumConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniSecret;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniServices;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
@OpJob("vacuum")
public class DbOpsVacuumJob extends DbOpsJob {

  @Inject
  public DbOpsVacuumJob(
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

  public DbOpsVacuumJob() {
    super(null, null, null, null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context) {
    StackGresDbOps dbOps = context.getSource();
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
                .withName(PatroniSecret.name(context.getCluster()))
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
  protected ClusterStatefulSetPath getRunScript() {
    return ClusterStatefulSetPath.LOCAL_BIN_RUN_VACUUM_SH_PATH;
  }

}
