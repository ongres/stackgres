/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
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
@DbOpsJob("majorVersionUpgrade")
public class DbOpsMajorVersionUpgradeJob extends AbstractDbOpsJob {

  private final ObjectMapper jsonMapper;

  @Inject
  public DbOpsMajorVersionUpgradeJob(
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
    this.jsonMapper = jsonMapper;
  }

  @Override
  protected List<EnvVar> getRunEnvVars(StackGresDbOpsContext context) {
    StackGresDbOps dbOps = context.getSource();
    StackGresDbOpsMajorVersionUpgrade majorVersionUpgrade =
        dbOps.getSpec().getMajorVersionUpgrade();
    return ImmutableList.<EnvVar>builder()
        .add(
            new EnvVarBuilder()
            .withName("TARGET_VERSION")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getPostgresVersion)
                .map(String::valueOf)
                .orElseThrow())
            .build(),
            new EnvVarBuilder()
            .withName("TARGET_EXTENSIONS")
            .withValue(Seq.seq(Optional.of(context.getCluster())
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPostgres)
                .map(StackGresClusterPostgres::getExtensions)
                .orElse(List.of()))
                .filter(extension -> Optional.ofNullable(majorVersionUpgrade)
                    .map(StackGresDbOpsMajorVersionUpgrade::getPostgresExtensions)
                    .stream()
                    .flatMap(List::stream)
                    .map(StackGresClusterExtension::getName)
                    .noneMatch(extension.getName()::equals))
                .append(Optional.ofNullable(majorVersionUpgrade)
                    .map(StackGresDbOpsMajorVersionUpgrade::getPostgresExtensions)
                    .orElse(List.of()))
                .transform(Optional::of)
                .map(Stream::toList)
                .map(jsonMapper::valueToTree)
                .map(Object::toString)
                .orElse("[]"))
            .build(),
            new EnvVarBuilder()
            .withName("TARGET_POSTGRES_CONFIG")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getSgPostgresConfig)
                .map(String::valueOf)
                .orElseThrow())
            .build(),
            new EnvVarBuilder()
            .withName("TARGET_BACKUP_PATH")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getBackupPath)
                .map(String::valueOf)
                .orElse(""))
            .build(),
            new EnvVarBuilder()
            .withName("LINK")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getLink)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CLONE")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getClone)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CHECK")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getCheck)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("MAX_ERRORS_AFTER_UPGRADE")
            .withValue(Optional.ofNullable(majorVersionUpgrade)
                .map(StackGresDbOpsMajorVersionUpgrade::getMaxErrorsAfterUpgrade)
                .map(String::valueOf)
                .orElse("false"))
            .build(),
            new EnvVarBuilder()
            .withName("CRD_GROUP")
            .withValue(CommonDefinition.GROUP)
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CRD_NAME")
            .withValue(HasMetadata.getPlural(StackGresCluster.class))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAMESPACE")
            .withValue(context.getSource().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAME")
            .withValue(context.getSource().getSpec().getSgCluster())
            .build(),
            new EnvVarBuilder()
            .withName("SERVICE_ACCOUNT")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "spec.serviceAccountName"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_CRD_NAME")
            .withValue(CustomResource.getCRDName(StackGresDbOps.class))
            .build(),
            new EnvVarBuilder()
            .withName("DBOPS_NAME")
            .withValue(dbOps.getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_POD_LABELS")
            .withValue(labelFactory.clusterLabels(context.getCluster())
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(",")))
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
            .build(),
            new EnvVarBuilder()
            .withName("MAJOR_VERSION_UPGRADE_CONTAINER_NAME")
            .withValue(StackGresInitContainer.MAJOR_VERSION_UPGRADE.getName())
            .build(),
            new EnvVarBuilder()
            .withName("POSTGRES_VERSION_KEY")
            .withValue(StackGresContext.POSTGRES_VERSION_KEY)
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_DURATION")
            .withValue(OperatorProperty.LOCK_DURATION.getString())
            .build(),
            new EnvVarBuilder()
            .withName("LOCK_SLEEP")
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
        .build();
  }

  @Override
  protected String getRunImage(StackGresDbOpsContext context) {
    return kubectl.getImageName(context.getSource());
  }

  @Override
  protected ClusterPath getRunScript() {
    return ClusterPath.LOCAL_BIN_RUN_MAJOR_VERSION_UPGRADE_SH_PATH;
  }

  @Override
  protected boolean isExclusiveOp() {
    return true;
  }

}
