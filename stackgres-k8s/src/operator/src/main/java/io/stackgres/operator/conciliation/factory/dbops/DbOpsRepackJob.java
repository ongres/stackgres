/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepack;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRepackConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
@DbOpsJob("repack")
public class DbOpsRepackJob extends AbstractDbOpsJob {

  @Inject
  public DbOpsRepackJob(
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
    StackGresDbOpsRepack repack = dbOps.getSpec().getRepack();
    List<EnvVar> runEnvVars = ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
                .withName("CLUSTER_NAMESPACE")
                .withValue(context.getSource().getMetadata().getNamespace())
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_NAME")
                .withValue(context.getSource().getMetadata().getName())
                .build(),
            new EnvVarBuilder()
                .withName("CLUSTER_PRIMARY_POD_LABELS")
                .withValue(labelFactory.clusterPrimaryLabels(context.getCluster())
                    .entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(",")))
                .build(),
            new EnvVarBuilder()
                .withName("PATRONI_CONTAINER_NAME")
                .withValue(StackGresContainer.PATRONI.getName())
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
  protected ClusterPath getRunScript() {
    return ClusterPath.LOCAL_BIN_RUN_REPACK_SH_PATH;
  }

  @Override
  protected String getRunImage(StackGresDbOpsContext context) {
    return kubectl.getImageName(context.getCluster());
  }
}
